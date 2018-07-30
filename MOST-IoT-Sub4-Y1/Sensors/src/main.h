#include <Arduino.h>
#include <avr/pgmspace.h>


#if defined(__AVR_ATmega328P__) || defined(__AVR_ATmega168__)
  #include <SoftwareSerial.h>
  SoftwareSerial BTSerial(4,3);

#elif defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
  #define BTSerial Serial2

#endif

//define HASH_SHA1
#define HASH_SHA1

#ifdef HASH_SHA1
#include <sha1.h>
#define printHash printHash1
#define SHA_BYTE_SIZE 20
#define Sha Sha1
uint8_t tk_sn[]={0xd4,0x46,0xa0,0xbe,0x48,0xc3,0x56,0x9d,0x1f,0x65,0x84,0xda,0xea,0xa1,0xea,0x76,0x09,0x7a,0x53,0xd9};
uint8_t sk_sn[]={0xaa,0x56,0x52,0x85,0x6d,0x59,0x1a,0xbe,0x89,0xbd,0xf0,0x43,0x8f,0xe5,0x7f,0x60,0x16,0x41,0xae,0xda};
#endif

#ifdef HASH_SHA256
#include <sha256.h>
#define printHash printHash256
#define SHA_BYTE_SIZE 32
#define Sha Sha256
uint8_t sk_sn[]={0x03,0x33,0x19,0xc1,0xa4,0x7a,0x0d,0x65,0x91,0x1f,0xa6,0x41,0x49,0xdf,0xfb,0xdd,0xa4,0x56,0x4f,0x52,0x93,0x6b,0xd5,0x13,0x18,0x81,0xf2,0x74,0xfc,0x1e,0x8c,0x44};
#endif



void printBytes(uint8_t* hash) {
  int i;
  for (i=0; i<4; i++) {
    Serial.print("0123456789abcdef"[hash[i]>>4]);
    Serial.print("0123456789abcdef"[hash[i]&0xf]);
  }
  Serial.println();
}


void printHash1(uint8_t* hash) {
  int i;
  for (i=0; i<20; i++) {
    Serial.print("0123456789abcdef"[hash[i]>>4]);
    Serial.print("0123456789abcdef"[hash[i]&0xf]);
  }
  Serial.println();
}

void printHash_S1(uint8_t* hash) {
  int i;
  for (i=0; i<20; i++) {
    BTSerial.print("0123456789abcdef"[hash[i]>>4]);
    BTSerial.print("0123456789abcdef"[hash[i]&0xf]);
  }
  BTSerial.println();
}

void printHash256(uint8_t* hash) {
  int i;
  for (i=0; i<32; i++) {
    Serial.print("0123456789abcdef"[hash[i]>>4]);
    Serial.print("0123456789abcdef"[hash[i]&0xf]);
  }
  Serial.println();
}

uint32_t getRandom()
{
  return random(0x1000000, 0xfffffff);
}

uint32_t getSubSha(uint8_t *sha)
{
  uint32_t sub=0;
  sub+=((uint32_t)sha[SHA_BYTE_SIZE-4])<<24;
  sub+=((uint32_t)sha[SHA_BYTE_SIZE-3])<<16;
  sub+=((uint32_t)sha[SHA_BYTE_SIZE-2])<<8;
  sub+=sha[SHA_BYTE_SIZE-1];
  return sub;
}

void copySha(uint8_t *src, uint8_t *dst)
{
  for(int i=0;i<SHA_BYTE_SIZE;i++)
  {
    dst[i]=src[i];
  }
}

void xorNumberWithSha(uint32_t x, uint8_t *sha)
{
  uint32_t sub_sk=getSubSha(sha);
  sub_sk=sub_sk^x;
  sha[SHA_BYTE_SIZE-4]=(sub_sk>>24) & 0xff;
  sha[SHA_BYTE_SIZE-3]=(sub_sk>>16) & 0xff;
  sha[SHA_BYTE_SIZE-2]=(sub_sk>>8) & 0xff;
  sha[SHA_BYTE_SIZE-1]=sub_sk & 0xff;
}
