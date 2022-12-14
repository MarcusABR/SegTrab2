package br.com.segcomp.rsa;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OAEP {
    final public int hLen;
    final public int K =256;
    final public MessageDigest digest;
    final public MGF1 mgf1;
    final public Random random;

    // public static void main(String[] args) {
    //     MessageDigest dg;
    //     try {
    //         dg = MessageDigest.getInstance("SHA-256");
    //         OAEP oaep = new OAEP(dg, new MGF1(dg), new SecureRandom());
    //         byte[] asd = oaep.padding("ayuashudasdasdasdasdasdasdasdqda", "asdasdasdasdasdasd");
    //         String xuy = oaep.depadding(asd,);

    //     } catch (NoSuchAlgorithmException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     } catch (IOException e) {
    //         // TODO Auto-generated catch block
    //         e.printStackTrace();
    //     }
        
    // }
    
    public OAEP(MessageDigest digest, MGF1 mgf1, Random random) {
        this.digest = digest;
        this.mgf1 = mgf1;
        this.random = random;
        hLen = digest.getDigestLength();
    }

    public String turnToHexcode(byte[] base){
        try {
            StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < base.length; i++) {
				String hex = Integer.toHexString(0xff & base[i]);
				if(hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
        } catch (Exception e) {
            // TODO: handle exception
            throw new RuntimeException(e);
        }
    }

    public byte[] padding(byte[] message, String label) throws IOException{
        int mLen = message.length;
        System.out.println("Tamanho da mensagem "+mLen);

        if(mLen >  K-hLen*2-2){
            System.out.println("Mensagem muito longa"); //TODO throw an error
        }

        byte[] lBytes = label.getBytes(StandardCharsets.UTF_8);
        byte[] lHash = digest.digest(lBytes);
        byte[] seed;
        do{
            seed = new BigInteger(256, random).toByteArray();
        }while(seed.length>32);

        byte[] ps = new byte[K - mLen - 2*hLen - 2]; //
        


        ByteArrayOutputStream os = new ByteArrayOutputStream( );
        byte[] db;
        os.write(lHash);
        os.write(ps);
        os.write(new byte[]{(byte)0x01});
        os.write(message);
        db = os.toByteArray();
        
        byte[] dbmask = mgf1.generateMaska(seed, K-hLen-1);
        byte[] maskedDB = xorBytes(db, dbmask);
        byte[] seedMask = mgf1.generateMaska(maskedDB, hLen);
        byte[] maskedSeed = xorBytes(seed, seedMask);

        os.reset();
        os.write(new byte[1]);
        os.write(maskedSeed);
        os.write(maskedDB);
        return os.toByteArray();          

    }

    private byte[] xorBytes(byte[] db, byte[] dbmask) {
        if(db.length != dbmask.length){
            throw new RuntimeException("Incompatible sizes");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        for (int i=0;i<db.length;i++) {
            outputStream.write(db[i]^dbmask[i]);
        }
        return outputStream.toByteArray();
    }

    public List<byte[]> depadding(byte[] cryptogram, int len) throws IOException{

        if(cryptogram.length!=K){
            throw new RuntimeException("Invalid cryptogram");
        } 

        byte[] maskedSeed =  new byte[hLen];
        byte[] maskedDB =  new byte[K-hLen-1];
        
        System.arraycopy(cryptogram, 1, maskedSeed, 0, hLen); 
        System.arraycopy(cryptogram, hLen+1, maskedDB, 0, K-hLen-1); 

        byte[] seedMask = mgf1.generateMaska(maskedDB, hLen);
        byte[] seed = xorBytes(maskedSeed, seedMask);
        

        byte[] dbMask = mgf1.generateMaska(seed, K-hLen-1);
        byte[] db = xorBytes(maskedDB, dbMask);
        
        StringBuffer hexString = new StringBuffer();

        for (int i = 0; i < db.length; i++) {
                String hex = Integer.toHexString(0xff & db[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
        
        byte[] label = new byte[hLen]; 
        byte[] msg = new byte[len]; 
        
        System.arraycopy(db, 0, label, 0, hLen); 
        System.arraycopy(db, K-len-hLen-1, msg, 0, len); 

        List<byte[]> result = new ArrayList<>();
        result.add(label);
        result.add(msg);
        return result;


    }
}
