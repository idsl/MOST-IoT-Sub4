#include "main.h"
#include <Arduino.h>

//#define SHT30_ADDR 0x44

#define bc 6000
#define bl 100
#define rb 3500
#define ebc 60

uint32_t id_sn = 0x7AB3A37E;
uint8_t mb[SHA_BYTE_SIZE];
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

float temp;
float humi;

//#define sk "ㄏㄏㄏㄏㄏㄏㄏㄏ"

#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
#include "DHT.h"
#define DHTPIN 6
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);
#elif defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)
#include "MAX30100_PulseOximeter.h"
#include <Wire.h>
PulseOximeter pox;
#endif

void authPhase1() {

  Serial.print("idsn:\t");
  Serial.println(id_sn);
  Serial.print("r1:\t");
  Serial.println(r1);
  uint8_t *idsn_byte = (uint8_t *)&id_sn;
  uint8_t *r1_byte = (uint8_t *)&r1;
  for (int i = 0; i < 4; i++)
    BTSerial.write(idsn_byte[i]);
  for (int i = 0; i < 4; i++)
    BTSerial.write(r1_byte[i]);
  uint32_t sub_sk = getSubSha(sk_sn);
  Sha.init();
  uint32_t r1_temp = sub_sk ^ r1;
  for (int i = 0; i < 16; i++) {
    Sha.write(sk_sn[i]);
  }

  uint8_t *vp = (uint8_t *)&r1_temp;
  for (int i = 3; i >= 0; i--) {
    Sha.write(vp[i]);
  }
  // Sha.print(sub_sk^r1);
  copySha(Sha.result(), temp_sha);

  xorNumberWithSha(rb, temp_sha);
  copySha(temp_sha, mb);
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    BTSerial.write(mb[i]);

  Serial.print("mb:\t");
  printHash(mb);

  Sha.init();
  Sha.print(rb);

  copySha(Sha.result(), temp_sha);
  xorNumberWithSha(v, temp_sha);
  copySha(temp_sha, x);
  Serial.print("x:\t");
  printHash(x);
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    BTSerial.write(x[i]);

  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(sk_sn[i]);
  }

  copySha(Sha.result(), temp_sha);
  // Serial.print("sk_sn:\t");
  // printHash(temp_sha);
  // Serial.print("v|id_sn:");
  // Serial.println((v|id_sn));
  xorNumberWithSha((v | id_sn), temp_sha);
  // Serial.print("vi:\t");
  // Serial.println((v | id_sn));

  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(temp_sha[i]);
  }
  // Serial.println();
  copySha(Sha.result(), m1);
  Serial.print("m1:\t");
  printHash(m1);
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    BTSerial.write(m1[i]);

  Sha.initHmac(sk_sn, SHA_BYTE_SIZE);
  Sha.print(id_sn);
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(x[i]);
  }
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(m1[i]);
  }
  Sha.print(r1);
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(mb[i]);
  }

  copySha(Sha.resultHmac(), m2);
  Serial.print("m2:\t");
  printHash(m2);
  for (int i = 0; i < SHA_BYTE_SIZE; i++)
    BTSerial.write(m2[i]);
  BTSerial.println();
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
      printHash(m3);
      while (BTSerial.available() < 20)
        ;
      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        m4[i] = BTSerial.read();
      }
      Serial.print("m4 : ");
      printHash(m4);
      while (BTSerial.available() < 20)
        ;

      for (int i = 0; i < SHA_BYTE_SIZE; i++) {
        y[i] = BTSerial.read();
      }
      Serial.print("y : ");
      printHash(y);
      while (BTSerial.available() < 4)
        ;

      for (int i = 0; i < 4; i++) {
        uint8_t temp = BTSerial.read();
        n1 = n1 << 8;
        n1 = n1 + temp;
      }
      Serial.print("n1 : ");
      Serial.println(n1);
      break;
    }
  }
}

int authPhase2() {

  Sha.initHmac(sk_sn, SHA_BYTE_SIZE);
  Sha.print(n1);
  Sha.print(r1);
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(y[i]);
  }
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(m3[i]);
  }
  uint8_t m4p[SHA_BYTE_SIZE];
  copySha(Sha.resultHmac(), m4p);
  Serial.print("m4p: ");
  printHash(m4p);

  uint32_t pre_w = getSubSha(sk_sn);
  pre_w = pre_w ^ n1;
  uint8_t *pre_w_bytes = (uint8_t *)&pre_w;
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE - 4; i++) {
    Sha.write(sk_sn[i]);
  }
  for (int i = 3; i >= 0; i--) {
    Sha.write(pre_w_bytes[i]);
  }
  uint8_t pre_y[SHA_BYTE_SIZE];
  copySha(Sha.result(), pre_y);
  uint32_t wp = 0;

  wp += ((uint32_t)(pre_y[16] ^ y[16])) << 24;
  wp += ((uint32_t)(pre_y[17] ^ y[17])) << 16;
  wp += ((uint32_t)(pre_y[18] ^ y[18])) << 8;
  wp += ((uint32_t)(pre_y[19] ^ y[19]));

  uint8_t tk_sni[SHA_BYTE_SIZE];
  copySha(sk_sn, tk_sni);
  xorNumberWithSha(v ^ wp, tk_sni);
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(tk_sni[i]);
  }
  copySha(Sha.result(), tk_sni);

  uint32_t pre_m3 = getSubSha(tk_sni) | id_sn;
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(sk_sn[i]);
  }
  uint8_t m3p[SHA_BYTE_SIZE];
  copySha(Sha.result(), m3p);
  xorNumberWithSha(pre_m3, m3p);
  Sha.init();
  for (int i = 0; i < SHA_BYTE_SIZE; i++) {
    Sha.write(m3p[i]);
  }
  copySha(Sha.result(), m3p);

  Serial.print("m3p: ");
  printHash(m3p);

  Serial.print("tk_sni: ");
  printHash(tk_sni);
  Serial.print("y : ");
  printHash(y);
  Serial.print("wp: ");
  Serial.println(wp);

  Serial.print("n1: ");
  Serial.print(n1);
  Serial.println();

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
    return true;
  } else {
    Serial.println("Sensor Authenticated Gateway failed");
    BTSerial.write(0xff);
    BTSerial.write(0xff);
    BTSerial.println();

    return false;
  }
}

void receiveAuth1() {
  while (true) {
    if (BTSerial.available() >= 2) {
      int init1 = BTSerial.read();
      int init2 = BTSerial.read();
      if (init1 == 0x00) {
        if (init2 == 0x33) {
          break;
        }
      }
    }
  }
}
#if defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)

long lastUpdate=0;

void onBeatDetected() {
  Serial.print("Heart rate:");
  Serial.print(pox.getHeartRate());
  Serial.print("bpm / SpO2:");
  Serial.print(pox.getSpO2());
  Serial.print("% / temp:");
  Serial.print(pox.getTemperature());
  Serial.println("C");
  humi = pox.getHeartRate();
  temp = pox.getSpO2();
  lastUpdate=millis();
}
#endif

void setup() {

  Serial.begin(115200);
  BTSerial.begin(9600);
#if defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  digitalWrite(6, HIGH);
  digitalWrite(5, LOW);
  Serial.print("Initializing pulse oximeter..");
  if (!pox.begin()) {
    Serial.println("FAILED");
    for (;;)
      ;
  } else {
    Serial.println("SUCCESS");
  }
  pox.setIRLedCurrent(MAX30100_LED_CURR_7_6MA);
  pox.setOnBeatDetectedCallback(onBeatDetected);

#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
  pinMode(19, OUTPUT);
  pinMode(18, OUTPUT);
  digitalWrite(19, HIGH);
  digitalWrite(18, LOW);

  // Sensor Init
  pinMode(5, OUTPUT);
  pinMode(4, OUTPUT);
  digitalWrite(5, HIGH);
  digitalWrite(4, LOW);
  dht.begin();
#endif

  randomSeed(analogRead(0) * analogRead(5));
  r1 = getRandom();
  v = getRandom();
}

bool authed = false;

void loop() {

#if defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)
  pox.update();
  if(millis()-lastUpdate>5000)
  {
    temp=0;
    humi=0;
  }
#endif
  if (!(BTSerial.available() < 2)) {
    int data = BTSerial.read();
    if (data == 0x00) {
      while (BTSerial.available() < 0)
        ;
      data = BTSerial.read();
      if (data == 0x33) {
        Serial.println("QQ2");
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
      }
    } else if (data == 0x44) {

      while (BTSerial.available() < 0)
        ;
      data = BTSerial.read();
      if (data == 0x33) {
        if (!authed) {
          BTSerial.write(0x44);
          for (int i = 0; i < 8; i++)
            BTSerial.write(0xff);
          return;
        }
#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
        humi = dht.readHumidity() + (((double)random(-50, 50)) / 100.0);
        temp = dht.readTemperature() + (((double)random(-50, 50)) / 100.0);
#endif
        Serial.println("Send data!!");
        BTSerial.write(0x44);
        byte *b_temp = (byte *)&temp;
        byte *b_humi = (byte *)&humi;
        for (int i = 0; i < 4; i++) {
          BTSerial.write(b_temp[i]);
        }
        for (int i = 0; i < 4; i++) {
          BTSerial.write(b_humi[i]);
        }
        BTSerial.println();
      }
    }
  }
}
