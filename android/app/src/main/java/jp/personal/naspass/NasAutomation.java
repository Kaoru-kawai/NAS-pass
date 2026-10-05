package jp.personal.naspass;

import android.net.Uri;
import org.json.JSONObject;

final class NasAutomation {
    static final String HOME = "https://mypage.nas-club.co.jp/mypage/?openExternalBrowser=1";
    static boolean trusted(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        return "https".equals(uri.getScheme()) && "mypage.nas-club.co.jp".equals(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getUserInfo() == null;
    }
    private static String quoted(String text) { return JSONObject.quote(text).replace("\u2028", "\\u2028").replace("\u2029", "\\u2029"); }
    static String script(JSONObject credentials, boolean allowLogin, boolean allowCard) throws Exception {
        String login = credentials != null && allowLogin ?
            "if(id&&pw&&login){ const form=pw.form; if(!form || new URL(form.action||location.href,location.href).origin!==location.origin)return 'unsafe';" +
            "const set=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;" +
            "set.call(id," + quoted(credentials.getString("member")) + ");set.call(pw," + quoted(credentials.getString("password")) + ");" +
            "[id,pw].forEach(e=>{e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));});" +
            "login.click();return 'login';}" : "";
        return "(()=>{if(location.protocol!=='https:'||location.hostname!=='mypage.nas-club.co.jp'||(location.port&&location.port!=='443'))return 'unsafe';" +
            "const id=document.getElementById('DN10InputID'),pw=document.querySelector('input[type=password]'),login=document.getElementById('DN10BtnLogin');" + login +
            "if(id&&pw)return 'login-required';" +
            (allowCard ? "const buttons=[document.getElementById('menuBtnKaiinSyo'),document.getElementById('menuItemAnchorKaiinsyo')];const card=buttons.find(e=>e&&e.getClientRects().length);if(card){card.click();return 'card';}" : "") +
            "return 'waiting';})()";
    }
}
