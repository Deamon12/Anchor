package com.sos.saveourstudents.supportclasses;

import android.text.TextUtils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Support class used with login and user creation checks
 */
    public class Validations  {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;

    private static final int FIRST_LAST_MAX_LENGTH = 20;

    public static final int REPEAT_NOT_SAME = 0;
    public static final int INCORRECT_LENGTH_TOP = 1;
    public static final int INCORRECT_LENGTH_BOT = 2;
    public static final int VALIDATION_PASSED = 3;

    public boolean testFirstLast(String incomingFirstLast) {
        if (incomingFirstLast.length() > FIRST_LAST_MAX_LENGTH) {
            return false;
        } else if (incomingFirstLast.length() < 0) {
            return false;
        } else {
            return true;
        }
    }


    public final static boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    public final static int areValidPasswords(String incomingPass1,String incomingPass2) {


        if (incomingPass1.length() > MAX_LENGTH || incomingPass1.length() < MIN_LENGTH) {
            return INCORRECT_LENGTH_TOP;
        }

        if (incomingPass2.length() > MAX_LENGTH || incomingPass2.length() < MIN_LENGTH) {
            return INCORRECT_LENGTH_BOT;
        }

        if (!(incomingPass1.equals(incomingPass2))) {
            return REPEAT_NOT_SAME;
        }

        return VALIDATION_PASSED;
    }

    public boolean testUserName(String incomingUser) {
        if (incomingUser.length() > MAX_LENGTH) {
            return false;
        } else if (incomingUser.length() < MIN_LENGTH) {
            return false;
        } else {
            return true;
        }
    }


    public int testPass(String incomingPass1,String incomingPass2) {
        //TextView passBottom, passBottom2;

        if (incomingPass1.length() > MAX_LENGTH || incomingPass1.length() < MIN_LENGTH) {
            //passBottom.setText(R.string.create_password_promp_err);
            return INCORRECT_LENGTH_TOP;
        }

        if (incomingPass2.length() > MAX_LENGTH || incomingPass2.length() < MIN_LENGTH) {
            //passBottom2.setText(R.string.create_password_promp_err);
            return INCORRECT_LENGTH_BOT;
        }

        if (!(incomingPass1.equals(incomingPass2))) {
            //passBottom2.setText(R.string.create_password_promp_notsame);
            //passBottom.setText(R.string.create_password_prompt);
            return REPEAT_NOT_SAME;
        }

        return VALIDATION_PASSED;
    }


    public static String get_SHA_1_SecurePassword(String passwordToHash){
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            //md.update(salt.getBytes()); //No salt....
            byte[] bytes = md.digest(passwordToHash.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++)
            {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return generatedPassword;
    }



}
