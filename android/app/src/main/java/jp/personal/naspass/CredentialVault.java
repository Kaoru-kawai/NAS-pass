package jp.personal.naspass;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Credentials are private to the app and encrypted with a non-exportable Android Keystore key. */
final class CredentialVault {
    private final String alias;
    private final String preferenceName;
    private final Context context;
    CredentialVault(Context context) { this(context, "nas-pass-credentials-v1", "vault"); }
    CredentialVault(Context context, String alias, String preferenceName) { this.context = context; this.alias = alias; this.preferenceName = preferenceName; }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(alias)) return (SecretKey) store.getKey(alias, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    void save(String member, String password) throws Exception {
        JSONObject json = new JSONObject().put("member", member).put("password", password);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String iv = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP);
        String encrypted = Base64.encodeToString(cipher.doFinal(json.toString().getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        if (!context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().putString("iv", iv).putString("data", encrypted).commit())
            throw new IllegalStateException("save failed");
    }
    JSONObject load() throws Exception {
        var prefs = context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE);
        String encrypted = prefs.getString("data", null);
        if (encrypted == null) return null;
        String iv = prefs.getString("iv", null);
        if (iv == null) throw new IllegalStateException("invalid vault");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        return new JSONObject(new String(cipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP)), StandardCharsets.UTF_8));
    }
    void clear() throws Exception {
        if (!context.getSharedPreferences(preferenceName, Context.MODE_PRIVATE).edit().clear().commit()) throw new IllegalStateException("clear failed");
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null); store.deleteEntry(alias);
    }
}
