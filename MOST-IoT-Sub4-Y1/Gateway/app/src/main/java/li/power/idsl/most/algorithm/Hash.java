package li.power.idsl.most.algorithm;

import android.util.Log;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Created by PowerLi on 2017/5/21.
 */
public class Hash {
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    private static final String HMAC_ALGORITHM=HMAC_SHA1_ALGORITHM;
    private static final String SHA1_ALGORITHM = "SHA-1";
    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String SHA384_ALGORITHM = "SHA-384";

    private static final String SHA_ALGORITHM=SHA1_ALGORITHM;
    public static final int SHA_SIZE=20;


    public static byte[] getHmac(byte[] key, Object ...data)
    {
        SecretKeySpec signingKey = new SecretKeySpec(key, HMAC_ALGORITHM);
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(signingKey);

            for(Object d : data)
            {
                if(d instanceof String)
                {
                    mac.update(((String) d).getBytes());
                }
                else if(d instanceof byte[])
                {
                    mac.update((byte[])d);
                }
                else if(d instanceof Integer)
                {
                    mac.update(d.toString().getBytes());
                }
                else
                {
                    Log.d("AUTH","Aitch");
                    return null;
                }
            }
            return mac.doFinal();


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static byte[] getSHA(Object ...data)
    {
        MessageDigest crypt = null;

        try {
            crypt=MessageDigest.getInstance(SHA_ALGORITHM);

            crypt.reset();

            for(Object d : data)
            {
                if(d instanceof String)
                {
                    crypt.update(((String) d).getBytes());
                }
                else if(d instanceof byte[])
                {
                    crypt.update((byte[]) d);
                }
                else if(d instanceof Integer)
                {
                    crypt.update(d.toString().getBytes());
                }
                else
                {
                    Log.d("AUTH","Citch");
                    return null;
                }
            }
            return crypt.digest();


        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] getXorWithSHA(byte[] sha, int x)
    {

        byte[] bytes = ByteBuffer.allocate(4).putInt(x).array();
        byte[] result=Arrays.copyOfRange(sha,0,20);
        for(int i=0;i<4;i++)
        {
            result[i+SHA_SIZE-4] =(byte)(sha[i+SHA_SIZE-4]^bytes[i]);
        }
        return result;
    }

    public static int getSubSHA(byte[] sha)
    {
        return ((sha[SHA_SIZE-4]&0xff)<<24)+((sha[SHA_SIZE-3]&0xff)<<16)+((sha[SHA_SIZE-2]&0xff)<<8)+(sha[SHA_SIZE-1]&0xff);
    }

    public static boolean compareByteArray(byte[] b1,byte[] b2)
    {

        for(int i = SHA_SIZE-1; i>0; i--)
        {
            if(b1[i]!=b2[i]) {
                return false;
            }
        }
        return true;
    }


    public static String bytesToHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for(byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static String getSha384(String data)
    {
        MessageDigest crypt = null;

        try {
            crypt=MessageDigest.getInstance(SHA384_ALGORITHM);

            crypt.reset();
            crypt.update(data.getBytes());

            } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        return bytesToHex(crypt.digest());

    }

}
