package li.power.idsl.most.algorithm;

import android.util.Log;
import li.power.idsl.most.app.sensor.Sensor;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

/**
 * Created by PowerLi on 2017/5/21.
 */
public class Algorithm {

    public static final byte[] SK_SN = {(byte) 0xaa, 0x56, 0x52, (byte) 0x85, 0x6d, 0x59, 0x1a, (byte) 0xbe, (byte) 0x89, (byte) 0xbd, (byte) 0xf0, 0x43, (byte) 0x8f, (byte) 0xe5, 0x7f, 0x60, 0x16, 0x41, (byte) 0xae, (byte) 0xda};
    public static final byte[] SHA_SK_SN = {0x3b, (byte) 0xf5, 0x32, 0x7c, (byte) 0xcf, (byte) 0xe0, 0x2a, 0x67, (byte) 0xe8, 0x6f, 0x41, (byte) 0xc4, 0x5f, 0x59, (byte) 0x0d, (byte) 0xd1, (byte) 0xa8, 0x4c, 0x5d, 0x55};

    public static int[] phase1(String name,byte[] data) {

        int idsn = ((data[5]&0xff) << 24) + ((data[4]&0xff) << 16) + ((data[3]&0xff) << 8) + (data[2]&0xff);
        int r1 = ((data[9]&0xff) << 24) + ((data[8]&0xff) << 16) + ((data[7]&0xff) << 8) + (data[6]&0xff);
        byte[] mb = Arrays.copyOfRange(data, 10, 30);
        byte[] x = Arrays.copyOfRange(data, 30, 50);
        byte[] m1 = Arrays.copyOfRange(data, 50, 70);
        byte[] m2 = Arrays.copyOfRange(data, 70, 90);

        Log.d(name+"-AUTH","ids:\t"+idsn);

        Log.d(name+"-AUTH"," xs:\t"+Hash.bytesToHex(x));

        Log.d(name+"-AUTH","mbs:\t"+Hash.bytesToHex(mb));

        Log.d(name+"-AUTH","m1s:\t"+Hash.bytesToHex(m1));

        Log.d(name+"-AUTH","m2s:\t"+Hash.bytesToHex(m2));


        byte[] m2p = Hash.getHmac(SK_SN, idsn, x, m1, r1, mb);
        Log.d(name+"-AUTH","m2p:\t"+Hash.bytesToHex(m2p));


        if (!Hash.compareByteArray(m2, m2p)) {
            Log.d(name+"-AUTH","m2 mismatch");
            return null;
        }

        byte[] mbp = Hash.getSHA(Hash.getXorWithSHA(SK_SN, r1));
        Log.d(name+"-AUTH","mbp:\t"+Hash.bytesToHex(mbp));

        int rb = Hash.getSubSHA(mb)^Hash.getSubSHA(mbp);
        Log.d(name+"-AUTH"," rb:\t"+String.valueOf(rb));

        byte[] rbHash = Hash.getSHA(rb);
        int v = Hash.getSubSHA(rbHash)^Hash.getSubSHA(x);

        byte[] m1p=Hash.getSHA(Hash.getXorWithSHA(SHA_SK_SN,(idsn | v)));
        Log.d(name+"-AUTH","m1p:\t"+Hash.bytesToHex(m1p));

        if(!Hash.compareByteArray(m1,m1p))
            return null;

        int[] result=new int[3];
        result[0]=idsn;
        result[1]=v;
        result[2]=r1;

        return result;
    }

    public static byte[] phase2(String name,int idsn, int v, int r1) {

        Random random=new Random();
        int w=0x01000000+random.nextInt(0x0fffffff);
        int n1=0x01000000+random.nextInt(0x0fffffff);
        Log.d(name+"-AUTH","  w:\t"+String.valueOf(w));
        Log.d(name+"-AUTH"," ni:\t"+String.valueOf(w));

        byte[] y=Hash.getXorWithSHA(Hash.getSHA(Hash.getXorWithSHA(SK_SN,n1)),w);
        Log.d(name+"-AUTH","  y:\t"+Hash.bytesToHex(y));

        byte[] tksni=Hash.getSHA(Hash.getXorWithSHA(SK_SN,v ^ w));
        Log.d(name+"-AUTH","tks:\t"+Hash.bytesToHex(tksni));

        byte[] m3=Hash.getSHA(Hash.getXorWithSHA(SHA_SK_SN,Hash.getSubSHA(tksni)|idsn));
        Log.d(name+"-AUTH"," m3:\t"+Hash.bytesToHex(m3));
        byte[] m4=Hash.getHmac(SK_SN,n1,r1,y,m3);
        Log.d(name+"-AUTH"," m4:\t"+Hash.bytesToHex(m4));

        byte[] toSensor=new byte[66];
        byte[] n1Bytes = ByteBuffer.allocate(4).putInt(n1).array();
        toSensor[0]=0x66;
        toSensor[1]=(byte)0x99;
        for(int i=0;i<20;i++)
        {
            toSensor[i+2]=m3[i];
            toSensor[i+20+2]=m4[i];
            toSensor[i+40+2]=y[i];
        }
        for(int i=0;i<4;i++)
        {
            toSensor[i+60+2]=n1Bytes[i];
        }

        return toSensor;
    }
}
