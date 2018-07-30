#include "main.h"
#include "DHT.h"
#include "DS3231.h"
#include "MAX30100_PulseOximeter.h"
#include <Arduino.h>
#include <Wire.h>

// define the pin and type of DHT
#define DHTPIN 8
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

//PulseOximeter pox;
uint32_t lastUpdate = 0;

//PM2.5
#include <SoftwareSerial.h>
#define pmsSerial Serial3

struct pms5003data {
  uint16_t framelen;
  uint16_t pm10_standard, pm25_standard, pm100_standard;
  uint16_t pm10_env, pm25_env, pm100_env;
  uint16_t particles_03um, particles_05um, particles_10um, particles_25um, particles_50um, particles_100um;
  uint16_t unused;
  uint16_t checksum;
};

struct pms5003data data;
boolean readPMSdata(Stream *s) {
  if (! s->available()) {
    return false;
  }

  // Read a byte at a time until we get to the special '0x42' start-byte
  if (s->peek() != 0x42) {
    s->read();
    return false;
  }

  // Now read all 32 bytes
  if (s->available() < 32) {
    return false;
  }

  uint8_t buffer[32];
  uint16_t sum = 0;
  s->readBytes(buffer, 32);

  // get checksum ready
  for (uint8_t i=0; i<30; i++) {
    sum += buffer[i];
  }

  /* debugging
  for (uint8_t i=2; i<32; i++) {
    Serial.print("0x"); Serial.print(buffer[i], HEX); Serial.print(", ");
  }
  Serial.println();
  */

  // The data comes in endian'd, this solves it so it works on all platforms
  uint16_t buffer_u16[15];
  for (uint8_t i=0; i<15; i++) {
    buffer_u16[i] = buffer[2 + i*2 + 1];
    buffer_u16[i] += (buffer[2 + i*2] << 8);
  }

  // put it into a nice struct :)
  memcpy((void *)&data, (void *)buffer_u16, 30);

  if (sum != data.checksum) {
    //Serial.println("Checksum failure");
    return false;
  }
  // success!
  return true;
}

// Declare the variables of Time
RTClib RTC;       // For getting real time clock function
DS3231 resetTime; // Define the current time
DateTime now;

// Sensors variables
float temp;
float humi;
float heartRate;
float spo2;
float bodyTemp;
uint32_t pm1_env;
uint32_t pm25_env;
uint32_t pm1_um;
uint32_t pm25_um;
//float pm25;

uint32_t id_sn = 0x7AB3A37E;
uint8_t mt[SHA_BYTE_SIZE];
uint8_t x[SHA_BYTE_SIZE];
uint8_t m1[SHA_BYTE_SIZE];
uint8_t m2[SHA_BYTE_SIZE];
uint8_t temp_sha[SHA_BYTE_SIZE];
uint8_t m3[SHA_BYTE_SIZE];
uint8_t m4[SHA_BYTE_SIZE];
uint32_t n1 = 0;
uint8_t y[SHA_BYTE_SIZE];
uint32_t r1;
uint32_t v;

// continous phase parameter
uint32_t r2;
uint8_t tk_sni[SHA_BYTE_SIZE];
uint8_t ms[SHA_BYTE_SIZE];
uint32_t m;
uint32_t sd = 3333;
uint8_t m5[SHA_BYTE_SIZE];
uint8_t y1[SHA_BYTE_SIZE];
uint8_t ack[SHA_BYTE_SIZE];
uint32_t n2 = 0;
//#define sk "ㄏㄏㄏㄏㄏㄏㄏㄏ"
bool authed = false;
bool initialAuth = false;
bool intAuthFlag = false;
bool entStatic = true;

bool ifJustLeftStatic = true;
bool ifSetTime = false;

//Setted Time
int year;
int month;
int day;
int hour;
int minute;
int second;

long startTime;
long diffTime;

//Master Sensors
byte maxTempBuffer[12];
byte hrtRateBuffer[4];
byte spO2Buffer[4];
byte bdyTempBuffer[4];

uint32_t settedUnixTime;

void authPhase1() {
  // Because that Serial.write() will only send out data type "byte"
  // To trasfer the data into bytes previously will be easier to do.

  Serial.print("idsn:\t");
  Serial.println(id_sn);
  Serial.print("r1:\t");
  Serial.println(r1);
  // uint8_t (0x00~0xff), so send 4 bytes is sufficient
  uint8_t *idsn_byte =
      (uint8_t *)&id_sn; // change the id_sn type from uint32_t to uint8_t
  uint8_t *r1_byte = (uint8_t *)&r1; // r1 is generated in the setup phase,
                                     // change from  type uint32_t to uint8_t
  for (int i = 0; i < 4; i++)        // Send Identitiy id_sn to the gateway
    BTSerial.write(idsn_byte[i]);
  for (int i = 0; i < 4; i++) // Send random number r1 to the gateway
    BTSerial.write(r1_byte[i]);
  // sk_sn is defined in main.h, where it can be the secret value from SHA1 or
  // SHA256
  uint32_t sub_sk =
      getSubSha(sk_sn); // Change sk_sn from type uint8_t to type uint32_t

  //***** Ready to generate mt = time XOR H(sk_sn XOR r1)

  Sha.init();                     // Initialize "Sha"
  uint32_t r1_temp = sub_sk ^ r1; // Ready to perform sk XOR r1
  for (int i = 0; i < 16; i++) {  // Hash sk_sn --> don't know why
    Sha.write(sk_sn[i]);
  }

  uint8_t *vp = (uint8_t *)&r1_temp;
  for (int i = 3; i >= 0; i--) { // Hash vp, which is (sk XOR r1)
    Sha.write(vp[i]);
  }

  // in the upper case, the r1_temp is recomposed

  // Sha.print(sub_sk^r1);
  copySha(Sha.result(),
          temp_sha); // make temp_sha = Sha.result() --> vp -->(sk XOR r1)

  // Change with the time
  xorNumberWithSha(now.unixtime(),
                   temp_sha); // time XOR temp_sha, the result will be temp_sha
  copySha(temp_sha, mt);      // make mb = temp_sha --> mt which is XORed
  for (int i = 0; i < SHA_BYTE_SIZE; i++) // Send mb(now mt) to the gateway
    BTSerial.write(mt[i]);

  Serial.print("mt:\t");
  printHash(mt);

  //******Ready to generate X = v XOR H(sk_sn)

  Sha.init();                // initialize Sha
  Sha.print(now.unixtime()); // Hash time

  copySha(Sha.result(), temp_sha); // make temp_sha = Sha.result() -->H(time)
  xorNumberWithSha(
      v, temp_sha); // random number v XOR temp_sha, the result will be temp_sha
  copySha(temp_sha, x); // make x = temp_sha -->(v XOR H(time))
  Serial.print("x:\t");
  printHash(x); // Hash table records x

  // Send the encrypted messages to the gateway
  for (int i = 0; i < SHA_BYTE_SIZE; i++) // Send X to the gateway
    BTSerial.write(x[i]);

  //******Ready to generate M1 = H(v||id_sn) XOR H(sk_sn)

  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // Hash sk_sn
    Sha.write(sk_sn[i]);
  }

  copySha(Sha.result(), temp_sha); // make temp_sha = H(sk_sn)
  // Serial.print("sk_sn:\t");
  // printHash(temp_sha);
  // Serial.print("v|id_sn:");
  // Serial.println((v|id_sn));
  xorNumberWithSha((v | id_sn),
                   temp_sha); //(v | id_sn) XOR temp_sha-->H(sk_sn), the result
                              // will be temp_sha
  // Serial.print("vi:\t");
  // Serial.println((v | id_sn));

  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE;
       i++) { // Hash temp_sha = (v||id_sn) XOR H(sk_sn)
    Sha.write(temp_sha[i]);
  }
  // Serial.println();
  copySha(Sha.result(), m1); // m1 = H(v||id_sn) XOR H(sk_sn)
  Serial.print("m1:\t");
  printHash(m1);                          // hash table remembers m1
  for (int i = 0; i < SHA_BYTE_SIZE; i++) // Send m1 to gateway
    BTSerial.write(m1[i]);

  //******Ready to generate M2 = HMAC(id_sn, X, M1, r1, mt)

  Sha.initHmac(
      sk_sn, SHA_BYTE_SIZE); // Initialize the HMAC funciton, the first variable
                             // will be the secret key, next is keyLength
  Sha.print(id_sn);          // Add id_sn to the buffer and do HMAC
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // Add X to the buffer and do HMAC
    Sha.write(x[i]);
  }
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // Add M1 to the buffer and do HMAC
    Sha.write(m1[i]);
  }
  Sha.print(r1);                            // Add r1 to the buffer and do HMAC
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // Add mt to the buffer and do HMAC
    Sha.write(mt[i]);
  }

  copySha(Sha.resultHmac(), m2); // m2 = Sha.resultHmac
  Serial.print("m2:\t");
  printHash(m2); // Hash table rememver m2
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    BTSerial.write(m2[i]); // Send M2 to the gateway
  BTSerial.println();
  Serial.print("ts: ");
  Serial.println(now.unixtime());
}

void receiveAuth2() {
  while (true) {
    if (BTSerial.available() > 2) {
      while (true) {
        if (BTSerial.read() == 0x66) {
          while (BTSerial.available() < 1)
            ;
          if (BTSerial.read() == 0x99) {
            break;
          }
        }
      }

      Serial.println("Got data from gateway");

      while (BTSerial.available() < 20)
        ;

      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        m3[i] = BTSerial.read();
      }
      Serial.print("m3 : ");
      printHash(m3); // Hash table rememver m3
      while (BTSerial.available() < 20)
        ;
      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        m4[i] = BTSerial.read();
      }
      Serial.print("m4 : ");
      printHash(m4); // Hash table rememver m4
      while (BTSerial.available() < 20)
        ;

      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        y[i] = BTSerial.read();
      }
      Serial.print("y : ");
      printHash(y); // Hash table rememver y
      while (BTSerial.available() < 4)
        ;

      for (int i = 0; i < 4; i++) {
        uint8_t temp = BTSerial.read();
        n1 = n1 << 8;
        n1 = n1 + temp;
      }
      Serial.print("n1 : ");
      Serial.println(n1); // Hash table rememver n1
      break;
    }
  }
}

int authPhase2() {

  Sha.initHmac(sk_sn, SHA_BYTE_SIZE);       // Initialize HAMC function
  Sha.print(n1);                            // Add n1 to the buffer and do HMAC
  Sha.print(r1);                            // Add r1 to the buffer and do HMAC
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // Add Y to the buffer and fo HMAC
    Sha.write(y[i]);
  }
  for (int i = 0; i < SHA_BYTE_SIZE;
       i++) { // Add M3 to the buffer and fo HMAC, M3 is from the recieve phase
    Sha.write(m3[i]);
  }
  uint8_t m4p[SHA_BYTE_SIZE];
  copySha(Sha.resultHmac(), m4p); // m4p = HMAC(n1, r1, Y, M3)
  Serial.print("m4p: ");
  printHash(m4p); // Add m4p to the hash table

  //******Ready to generate w' = HMAC(id_sn, X, M1, r1, mt)
  // first you have to know Y
  uint32_t pre_w = getSubSha(sk_sn);
  pre_w = pre_w ^ n1;
  uint8_t *pre_w_bytes = (uint8_t *)&pre_w;
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE - 4;
       i++) { // write sk_sn into buffer and hash
    Sha.write(sk_sn[i]);
  }
  for (int i = 3; i >= 0;
       i--) { // Write pre_w_bytes into buffer --> (sk_sn XOR n1) and Hash
    Sha.write(pre_w_bytes[i]);
  }
  uint8_t pre_y[SHA_BYTE_SIZE];
  copySha(Sha.result(), pre_y);
  // w' from uint32_t to uint8_t
  uint32_t wp = 0;

  wp += ((uint32_t)(pre_y[16] ^ y[16])) << 24;
  wp += ((uint32_t)(pre_y[17] ^ y[17])) << 16;
  wp += ((uint32_t)(pre_y[18] ^ y[18])) << 8;
  wp += ((uint32_t)(pre_y[19] ^ y[19]));
  // Get W'

  //******Ready to generate tk_sni = H(v XOR w' XOR sk_sn)
  copySha(sk_sn, tk_sni);
  xorNumberWithSha(v ^ wp,
                   tk_sni); // tk_sni used to be the same as sk_sn, but now
                            // returned to tk_sni = v XOR w' XOR sk_sn
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(tk_sni[i]); // Hash tk_sni and but not outputed
  }
  copySha(Sha.result(), tk_sni); // Give the result to tk_sni

  //******Ready to generate M3' = H((tk_sni || id_sn)) XOR H(sk_sn)
  uint32_t pre_m3 = getSubSha(tk_sni) | id_sn;
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) { // sk_sn add to buffer and hash
    Sha.write(sk_sn[i]);
  }
  uint8_t m3p[SHA_BYTE_SIZE];
  copySha(Sha.result(), m3p);    // m3p = H(sk_sn)
  xorNumberWithSha(pre_m3, m3p); //(tk_sni || id_sn) XOR H(sk_sn)
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE;
       i++) { // Hash ((tk_sni || id_sn) XOR H(sk_sn))
    Sha.write(m3p[i]);
  }
  copySha(Sha.result(), m3p); // make m3p = H((tk_sni || id_sn) XOR H(sk_sn))

  Serial.print("m3p: ");
  printHash(m3p); // Store m3p in the hash table

  Serial.print("tk_sni: ");
  printHash(tk_sni); // Store tk_sni in the hash table
  Serial.print("y : ");
  printHash(y); // Store Y in the hash table
  Serial.print("wp: ");
  Serial.println(wp);

  bool m3_ok = true;
  bool m4_ok = true;
  for (int i = SHA_BYTE_SIZE - 1; i > 0; i--) {
    if (m3[i] != m3p[i]) {
      m3_ok = false;
      break;
    }
  }
  for (int i = SHA_BYTE_SIZE - 1; i > 0; i--) {
    if (m4[i] != m4p[i]) {
      m4_ok = false;
      break;
    }
  }
  if (m3_ok && m4_ok) {
    Serial.println("Sensor Authenticated Gateway successfully");
    BTSerial.write(0xff);
    BTSerial.write((uint8_t)0x00);
    BTSerial.println();

    // trans m to w'
    // uint8_t *wp_breaker = (uint8_t*)&wp;
    // for(int i=SHA_BYTE_SIZE-1; i>SHA_BYTE_SIZE-5;i++)
    //   m[i] = wp_breaker[i];
    m = wp;
    Serial.print("m: ");
    Serial.println(m);

    return true;
  } else {
    Serial.println("Sensor Authenticated Gateway failed");
    BTSerial.write(0xff);
    BTSerial.write(0xff);
    BTSerial.println();

    return false;
  }
}

void contAuthPhase1() {
  uint8_t *idsn_byte = (uint8_t *)&id_sn;
  for (int i = 0; i < 4; i++) {
    BTSerial.write(idsn_byte[i]); // Send out message
  }
  Serial.print("idsn:\t");
  printHash(idsn_byte);
  uint8_t *r2_byte = (uint8_t *)&r2;
  for (int i = 0; i < 4; i++) {
    BTSerial.write(r2_byte[i]);
    // Serial.print(r2_byte[i],HEX);         //Send out message
  }
  Serial.print("r2:\t");
  Serial.println(r2);

  // Declare a uint32_t varible
  uint32_t temp_Sha32;
  // Generate mt = time ^ H(tk_sni | (m^r2))

  temp_Sha32 = m ^ r2;
  // Serial.print("m ^ r2: ");
  // Serial.println(m ^ r2);
  uint32_t sub_tk = getSubSha(tk_sni);

  //Serial.print("getSubSHA(tk_sni,HEX): ");
  // for(int i=0;i<SHA_BYTE_SIZE;i++)
  //Serial.println(getSubSha(tk_sni), HEX);

  temp_Sha32 = sub_tk | temp_Sha32;
  //Serial.print("OR Bitch: ");
  //Serial.println(temp_Sha32, HEX);

  Sha.init();
  uint8_t *temp_Sha32_bytes = (uint8_t *)&temp_Sha32;
  for (int i = 3; i >= 0; i--) {
    Sha.write(temp_Sha32_bytes[i]);
  }
  // Sha.print(temp_Sha32);
  // Serial.println("Not Stuck Here 0");
  copySha(Sha.result(), temp_sha); // Store bytes at the end
  //Serial.print("H(tk_sni|(m^r2)): ");
  // for (int i = 0; i < SHA_BYTE_SIZE; i++) {
  //   Serial.print(temp_sha[i], HEX);
  // }
  // Serial.println("");
  xorNumberWithSha(now.unixtime(), temp_sha);
  // Serial.println("Not Stuck Here 2");
  copySha(temp_sha, mt);
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    // Serial.print(mt[i],HEX);
    BTSerial.write(mt[i]); // Send out message
  }
  Serial.print("mt:\t");
  printHash(mt);

  // Generate ms = sd ^ H((tk_sni^m)|r2)
  Sha.init();
  temp_Sha32 = getSubSha(tk_sn) ^ m;
  temp_Sha32 = temp_Sha32 | r2;
  // for(int i=0;i<SHA_BYTE_SIZE-4;i++)
  //   Sha.write(sk_sn[i]);
  // uint8_t *ms_temp =(uint8_t*)&temp_Sha32;
  // for(int i=3; i>=0;i--)
  //   Sha.write(ms_temp[i]);
  Sha.print(temp_Sha32);
  copySha(Sha.result(), temp_sha); // Store bytes at the end
  xorNumberWithSha(sd, temp_sha);
  copySha(temp_sha, ms);
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    // Serial.print(ms[i],HEX);
    BTSerial.write(ms[i]); // Send out message
  }
  Serial.print("ms:\t");
  printHash(ms);

  // Generate m5 = HMAC(id_sn, ms, mt, r2)
  Sha.initHmac(sk_sn, SHA_BYTE_SIZE);
  Sha.print(id_sn); // Add id_sn to buffer
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    Sha.write(ms[i]); // Add ms to the buffer
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    Sha.write(mt[i]);            // Add mt to the buffer
  Sha.print(r2);                 // Add r2 to the buffer
  copySha(Sha.resultHmac(), m5); // Add the result to m5
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    // Serial.print(m5[i],HEX);
    BTSerial.write(m5[i]); // Send out message
  }

  Serial.print("M5:\t");
  printHash(m5);

  // Show generated time
  Serial.print("tc: ");
  Serial.println(now.unixtime());
}
void contAuthPhase2() {
  Serial.println("Enter second continous phase");
  // find n2p
  Sha.init();
  uint32_t sub_tk = getSubSha(tk_sni);
  uint32_t temp_Sha32;
  temp_Sha32 = sub_tk ^ r2;

  // Serial.print("tk_sni^r2: ");
  // Serial.println(temp_Sha32,HEX);
  Serial.print("m: ");
  Serial.println(m);

  temp_Sha32 = temp_Sha32 | m;
  // Serial.print("tk_sni^r2|m: ");
  // Serial.println(temp_Sha32);
  // uint8_t *n2p_temp = (uint8_t*)&temp_Sha32;
  // for(int i =0;i<SHA_BYTE_SIZE;i++)
  //  Sha.write(n2p_temp[i]);
  Sha.init();
  uint8_t *temp_Sha32_bytes = (uint8_t *)&temp_Sha32;
  for (int i = 3; i >= 0; i--) {
    Sha.write(temp_Sha32_bytes[i]);
  }
  // Sha.print(temp_Sha32);
  // Serial.println("Not Stuck Here 0");
  //copySha(Sha.result(), temp_sha); // Store bytes at the end
  // Serial.print("H(tk_sni|(m^r2)): ");
  // for (int i = 0; i < SHA_BYTE_SIZE; i++) {
  //   Serial.print(temp_sha[i], HEX);
  // }
  // Serial.println("");

  //Sha.print(temp_Sha32);
  uint8_t pre_y[SHA_BYTE_SIZE];
  copySha(Sha.result(), pre_y);

  Serial.print("pre_y: ");
  for(int i=0; i<SHA_BYTE_SIZE;i++){
    Serial.print(pre_y[i],HEX);
  }Serial.println();

  uint32_t n2p = 0;
  n2p += ((uint32_t)(pre_y[16] ^ y1[16])) << 24;
  n2p += ((uint32_t)(pre_y[17] ^ y1[17])) << 16;
  n2p += ((uint32_t)(pre_y[18] ^ y1[18])) << 8;
  n2p += ((uint32_t)(pre_y[19] ^ y1[19]));

  Serial.print("n2p:\t");
  Serial.println(n2p);

  uint32_t m_tk = m | sub_tk;
  Serial.print("m|tk:\t");
  Serial.println(m_tk);
  xorNumberWithSha(m_tk, pre_y);

  if (n2p == m_tk) {
    Serial.println("Enter static authentication check");
    temp_Sha32 = m ^ now.unixtime();
    temp_Sha32 = temp_Sha32 | (now.unixtime() ^ r2);
    temp_Sha32 = temp_Sha32 | (m | sub_tk);
    Sha.init();
    uint8_t *temp_Sha32_bytes = (uint8_t *)&temp_Sha32;
    for (int i = 3; i >= 0; i--) {
      Sha.write(temp_Sha32_bytes[i]);
    }
    uint8_t ackp[SHA_BYTE_SIZE];
    copySha(Sha.result(), ackp);
    Serial.print("ackp:\t");
    for (int i = 0; i < SHA_BYTE_SIZE; i++) {
      Serial.print(ackp[i], HEX);
    }
    Serial.println("");
    // printHash(ackp);
    authed = true;
    for (int i = SHA_BYTE_SIZE - 1; i > 0; i--) {
      if (ackp[i] != ack[i]) {
        Serial.println("gateway authentication failed 1");
        authed = false;

        while (true)
          ;
        break;
      }
    }
    if (authed) {
      Serial.println("Gateway continuous authentication succcess");
      Serial.println("==Next phase static authentication==");
      BTSerial.write(0xff);
      BTSerial.write((uint8_t)0x00);
      BTSerial.println();
      authed = true;
      entStatic = true;
    }
  } else {

    temp_Sha32 = m ^ now.unixtime();
    temp_Sha32 = temp_Sha32 | (n2p ^ r2);
    temp_Sha32 = temp_Sha32 | (m ^ sub_tk);

    // for(int i=0;i<SHA_BYTE_SIZE-4;i++)
    //   Sha.write(sk_sn[i]);
    // uint8_t *ackp_temp = (uint8_t*)&temp_Sha32;
    // for(int i=3; i>0;i--)
    //  Sha.write(ackp_temp[i]);
    Sha.init();
    uint8_t *temp_Sha32_bytes = (uint8_t *)&temp_Sha32;
    Serial.print("TS: ");

    for (int i = 3; i >= 0; i--) {
      Sha.write(temp_Sha32_bytes[i]);
      Serial.print(temp_Sha32_bytes[i],HEX);
    }

    Serial.println();
    uint8_t ackp[SHA_BYTE_SIZE];
    copySha(Sha.result(), ackp);
    Serial.print("ackp:\t");
    for (int i = 0; i < SHA_BYTE_SIZE; i++) {
      Serial.print(ackp[i], HEX);
    }
    Serial.println("");

    authed = true;
    for (int i = SHA_BYTE_SIZE - 1; i > 0; i--) {
      if (ackp[i] != ack[i]) {

        Serial.print("Failed on index:");
        Serial.println(i);

        BTSerial.write(0xff);
        BTSerial.write(0xff);
        BTSerial.println();
        authed = false;
        while (true)
          ;
        break;
      }
    }
    if (authed) {
      Serial.println("gatway continuous authentication success");
      m = n2p;
      Serial.print("m:\t");
      Serial.println(m);
      BTSerial.write(0xff);
      BTSerial.write((uint8_t)0x00);
      BTSerial.println();
      // uint8_t *n2p_breaker = (uint8_t*)&n2p;
      // for(int i=SHA_BYTE_SIZE-1; i>SHA_BYTE_SIZE-5; i--)
      //   m[i]=n2p_breaker[i]; //make m's tail change to n2p 4 bytes
      authed = true;
      // while (true)
    }
  }
}

void recieveContAuth2() {
  while (true) {
    if (BTSerial.available() > 2) {
      while (true) {
        if (BTSerial.read() == 0x66) {
          while (BTSerial.available() < 1)
            ;
          if (BTSerial.read() == 0x99) {
            break;
          }
        }
      }
      Serial.println("Got data from gateway");

      // Serial.println("Got bytes:"+BTSerial.available());

      while (BTSerial.available() < 20) {
        delay(1000);
      }
      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        y1[i] = BTSerial.read();
      }

      Serial.print("y1:\t");
      printHash(y1);
      // Serial.println("Waiting for ack");
      while (BTSerial.available() < 20)
        ;
      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        ack[i] = BTSerial.read();
      }
      Serial.print("ack:\t");
      printHash(ack);
      break;
    }
  }
}

bool setTime(int randYear, int randMonth, int randDay, int randHour, int randMinute, int randSecond){

  if(randYear<1970){
    // Serial.print("Year Uncorrect: ");
    // Serial.println(randYear);
    return false;
  }//Year
  if(randMonth>12 || randMonth<1){
    Serial.print("Month Uncorrect: ");
    //Serial.println(month);
    return false;
  } //Month
  if(randDay>28 || randDay<1){
    Serial.print("Day Uncorrect: ");
    //Serial.println(day);
    return false;
  } //Day
  if(randHour>23 || randHour<0){
    Serial.print("Hour Uncorrect: ");
    //Serial.println(hour);
    return false;
  } //Hour
  if(randMinute>59 || randMinute<0){
    Serial.print("Minute Uncorrect: ");
    //Serial.println(minute);
    return false;
  }//minute
  if(randSecond>59 || randSecond<0){
    Serial.print("Second Uncorrect: ");
    //Serial.println(second);
    return false;
  } //Second
  else{
    year = randYear;
    month = randMonth;
    day = randDay;
    hour = randHour;
    minute = randMinute;
    second = randSecond;
  }
    return true;
}

void doSetTime(){
  while(BTSerial.available()<7); //Wait to receive time variables
  uint8_t time[7];
  for(int i=0;i<7;i++)
    time[i]=BTSerial.read();
  //combine time[0], time[1] to reform the year
  startTime = millis();
  int year = time[0]*100+time[1];

  if(setTime(year, time[2], time[3], time[4], time[5], time[6])){
    Serial.println("Random Time setted");
    BTSerial.write(0x00); //Notify gateway the time is set
    // Serial.print("Year: ");Serial.println(year);
    // Serial.print("Month: ");Serial.println(month);
    // Serial.print("Day: ");Serial.println(day);
    // Serial.print("Hour: ");Serial.println(hour);
    // Serial.print("Minute: ");Serial.println(minute);
    // Serial.print("Second: ");Serial.println(second);

    //settedUnixTime = toUnixTime(year, time[2], time[3], time[4], time[5], time[6]);

    //BTSerial.write(0x00);
    ifSetTime = true;
  }
}

void setup() {

  Serial.begin(115200);
  BTSerial.begin(9600);
  pmsSerial.begin(9600);

 //DHT
  pinMode(10, OUTPUT);
  pinMode(9, OUTPUT);
  digitalWrite(10, LOW);
  digitalWrite(9, HIGH);
  dht.begin();
//#endif

  //randomSeed(analogRead(0) * analogRead(5));

  r1 = getRandom();
  r2 = getRandom();
  v = getRandom();
  // Reset the time
  Wire.begin();
  Serial.println("Ready for Data...");

}

void loop() {

  //Master code Recieve from slave
  Wire.requestFrom(8, 12);
  while(Wire.available()){

    for(int i=0; i<12; i++){
      maxTempBuffer[i] = Wire.read();
    }
  }

    for(int i = 0; i < 4;i++)
      hrtRateBuffer[i] = maxTempBuffer[i];
    for(int i = 0; i < 4; i++)
      spO2Buffer[i] = maxTempBuffer[i+4];
    for(int i = 0; i < 4; i++)
      bdyTempBuffer[i] = maxTempBuffer[i+8];

  if (readPMSdata(&pmsSerial)) {
    // reading data was successful!
    pm1_env = data.pm10_env;
    pm25_env = data.pm25_env;
    pm1_um = data.particles_10um;
    pm25_um = data.particles_25um;
  }

  if (!(BTSerial.available() < 2)) {
    //Set initial time
    if(!ifSetTime){

      while (true) {

        Serial.println("Entered");

        if (BTSerial.read() == 0x11) {
          while (BTSerial.available() < 1)
            ;
          if (BTSerial.read() == 0x22) {
            break;
          }
        }
      }

      Serial.println("Got initial time from gateway");
      doSetTime();

          int adj_year = year-2018;

            resetTime.setYear(char(1970+adj_year));
            resetTime.setMonth(char(month));
            resetTime.setDate(char(day));
            resetTime.setHour(char(hour));
            resetTime.setMinute(char(minute));
            resetTime.setSecond(char(second));

            DateTime now = RTC.now();

        Serial.print("Recieve unix time:");
        Serial.println(now.unixtime());

    }

    //Authentication
    int data = BTSerial.read();

    if (data == 0x00) {
      while (BTSerial.available() < 0)
        ;
      data = BTSerial.read();
      if (data == 0x33) { // Authentication Section
        Serial.println("QQ2");

        if (entStatic) { // do Static authentication

          BTSerial.write(0x33);
          BTSerial.write(0x66);

          Serial.println("Auth phase 1 started");
          long auth1 = millis();
          authPhase1();
          Serial.print("Auth 1 done time=");
          Serial.println(millis() - auth1);

          receiveAuth2();
          Serial.println("Auth phase 2 started");
          long auth2 = millis();
          if (authPhase2()) {
            authed = true;
          }
          Serial.print("Auth 2 done time=");
          Serial.println(millis() - auth2);

          // Finish first Static authentication
          r1 = getRandom(); // r1 type is uint32_t
          v = getRandom();  // v type is uint32_t
          entStatic = false;
          ifJustLeftStatic = true;

          delay(100);
          BTSerial.write(0x77);

        } else {
          BTSerial.write(0x11);
          BTSerial.write(0x22);

          Serial.println("CAuth phase 1 started");
          long auth1 = millis();
          contAuthPhase1();
          Serial.print("CAuth 1 done time=");
          Serial.println(millis() - auth1);

          recieveContAuth2();
          Serial.println("CAuth phase 2 started");
          long auth2 = millis();
          contAuthPhase2();
          Serial.print("CAuth 2 done time=");
          Serial.println(millis() - auth2);

          // Finish first continous authentication
          r2 = getRandom();

          //Send notification to gateway prepare to receive data
          delay(100);
          BTSerial.write(0x77);

        }
      }
    } else if (data == 0x44) {

      while (BTSerial.available() < 0)
        ;
      data = BTSerial.read();
      if (data == 0x33) { // Recieve 0x44 and 0x33, ready to send data
        if (!authed) {
          BTSerial.write(0x44);
          for (int i = 0; i < 8; i++)
            BTSerial.write(0xff);
          return;
        }
        //DHT display
        //#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
        humi = dht.readHumidity() + (((double)random(-50, 50)) / 100.0);
        temp = dht.readTemperature() + (((double)random(-50, 50)) / 100.0);
        //
        Serial.println("---------------------------------------");
        Serial.print("Temperature: ");Serial.println(temp);
        Serial.print("Humidity:");Serial.println(humi);
        Serial.println("---------------------------------------");
        Serial.print("PM 1.0: "); Serial.print(pm1_env);
        Serial.print("\t\tPM 2.5: "); Serial.println(pm25_env);
        Serial.print("Particles > 1.0um / 0.1L air:"); Serial.println(pm1_um);
        Serial.print("Particles > 2.5um / 0.1L air:"); Serial.println(pm25_um);
        Serial.println("---------------------------------------");
        //#endif
        Serial.println("Send data!!");
        BTSerial.write(0x44);

        byte *b_temp = (byte *)&temp;
        byte *b_humi = (byte *)&humi;

        byte *b_pm25 = (byte *)&pm25_env;

        for (int i = 0; i < 4; i++) {
          BTSerial.write(hrtRateBuffer[i]);
        }
        for (int i = 0; i < 4; i++) {
          BTSerial.write(spO2Buffer[i]);
        }
        for (int i = 0; i < 4; i++) {
          BTSerial.write(bdyTempBuffer[i]);
        }

        for (int i = 0; i < 4; i++) {
          BTSerial.write(b_temp[i]);
        }
        for (int i = 0; i < 4; i++) {
          BTSerial.write(b_humi[i]);
        }

        for (int i = 0; i < 4; i++) {
          BTSerial.write(b_pm25[i]);
        }

        BTSerial.println();
      }
    }
  }else{
    delay(1000);
  }


  // End of reading bluetooth reading
  // The time will count on and on
  now = RTC.now();


}
