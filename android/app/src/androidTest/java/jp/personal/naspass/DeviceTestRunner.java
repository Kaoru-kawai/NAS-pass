package jp.personal.naspass;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import java.io.File;
import java.io.FileOutputStream;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Form tests use synthetic data and never submit to NAS. MainActivity retains its normal sign-in behavior. */
public class DeviceTestRunner extends Instrumentation {
    private WebView view;
    private final StringBuilder report = new StringBuilder();
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private void check(boolean condition,String name) { if(!condition)throw new AssertionError(name); report.append("PASS ").append(name).append('\n'); }
    @Override public void onStart() {
        Bundle result=new Bundle();int code=Activity.RESULT_OK;
        try {
            check(NasAutomation.trusted(NasAutomation.HOME),"official HTTPS host accepted");
            check(!NasAutomation.trusted("https://mypage.nas-club.co.jp.evil.example/mypage/"),"lookalike host blocked");
            check(!NasAutomation.trusted("http://mypage.nas-club.co.jp/mypage/"),"HTTP blocked");
            check(!NasAutomation.trusted("https://mypage.nas-club.co.jp:444/mypage/"),"unexpected port blocked");
            CredentialVault vault=new CredentialVault(getTargetContext(),"nas-pass-test-key","vault-test");
            try {
                vault.save("test-member","synthetic-password");
                check("synthetic-password".equals(vault.load().getString("password")),"Android Keystore encrypt/decrypt round trip");
                check(!getTargetContext().getSharedPreferences("vault-test",0).getString("data","").contains("synthetic-password"),"stored credentials are ciphertext");
            } finally { vault.clear(); }
            check(vault.load()==null,"credential deletion");
            JSONObject creds=new JSONObject().put("member","test-'member").put("password","test-\"password\\");
            page("<form action='/mypage/'><input id='DN10InputID'><input type='password'><button type='button' id='DN10BtnLogin' onclick='window.submitted=true'>Login</button></form>");
            check("\"login\"".equals(evaluate(NasAutomation.script(creds,true,true))),"login form submitted");
            check("true".equals(evaluate("window.submitted===true && document.getElementById('DN10InputID').value==="+JSONObject.quote(creds.getString("member"))+" && document.querySelector('input[type=password]').value==="+JSONObject.quote(creds.getString("password")))),"credential quoting preserves special characters");
            check("\"login-required\"".equals(evaluate(NasAutomation.script(creds,false,true))),"automatic retries disabled after attempt");
            page("<form action='https://evil.example/'><input id='DN10InputID'><input type='password'><button id='DN10BtnLogin'>Login</button></form>");
            check("\"unsafe\"".equals(evaluate(NasAutomation.script(creds,true,true))),"cross-origin form blocked before entering credentials");
            check("true".equals(evaluate("document.querySelector('input[type=password]').value===''")),"unsafe form stays empty");
            page("<button id='menuBtnKaiinSyo' style='display:none'>Hidden</button><a id='menuItemAnchorKaiinsyo' onclick='window.card=true'>会員証</a>");
            check("\"card\"".equals(evaluate(NasAutomation.script(null,false,true))) && "true".equals(evaluate("window.card===true")),"visible membership link selected");
            check("\"waiting\"".equals(evaluate(NasAutomation.script(null,false,false))),"membership opening does not repeat");
            runOnMainSync(()->{
                Bitmap source=Bitmap.createBitmap(400,200,Bitmap.Config.ARGB_8888);
                for(int y=0;y<200;y++)for(int x=0;x<400;x++)source.setPixel(x,y,Color.rgb(x%256,y,0));
                SquareCropView selection=new SquareCropView(getTargetContext());selection.layout(0,0,800,800);selection.setBitmap(source);
                Bitmap center=selection.export();check(center.getWidth()==center.getHeight()&&center.getWidth()==160,"default crop is square in source pixels");center.recycle();
                selection.resize(1);selection.move(-10000,-10000);Bitmap left=selection.export();
                check(left.getWidth()==200&&left.getPixel(0,0)==source.getPixel(0,0),"crop clamps to top-left image edge");left.recycle();
                selection.move(10000,10000);Bitmap right=selection.export();
                check(right.getWidth()==200&&right.getPixel(0,0)==source.getPixel(200,0),"crop clamps to bottom-right image edge");right.recycle();
                selection.resize(.5f);Bitmap smaller=selection.export();check(smaller.getWidth()==100&&smaller.getHeight()==100,"crop size control maps correctly");
                float[] saved=selection.selection();SquareCropView restored=new SquareCropView(getTargetContext());restored.layout(0,0,600,900);restored.setBitmap(source);restored.restoreSelection(saved);Bitmap again=restored.export();
                check(again.getWidth()==smaller.getWidth()&&again.getPixel(0,0)==smaller.getPixel(0,0),"crop selection survives viewport changes");smaller.recycle();again.recycle();source.recycle();
            });
            File cropSource=new File(getTargetContext().getFilesDir(),"crop-source"),cropOutput=new File(getTargetContext().getFilesDir(),"crop-output");
            if(!cropSource.exists()&&!cropOutput.exists()){
                try{
                    Bitmap sample=Bitmap.createBitmap(320,180,Bitmap.Config.ARGB_8888);sample.eraseColor(Color.rgb(235,239,250));
                    Canvas demo=new Canvas(sample);android.graphics.Paint p=new android.graphics.Paint();p.setColor(Color.rgb(33,91,214));demo.drawRect(70,20,250,160,p);
                    try(FileOutputStream stream=new FileOutputStream(cropSource)){sample.compress(Bitmap.CompressFormat.PNG,100,stream);}sample.recycle();
                    Intent cropIntent=new Intent(getTargetContext(),CropActivity.class);cropIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Activity cropActivity=startActivitySync(cropIntent);waitForIdleSync();
                    AtomicReference<Boolean> loaded=new AtomicReference<>(false);
                    for(int attempt=0;attempt<50&&!loaded.get();attempt++){runOnMainSync(()->loaded.set(find(cropActivity.findViewById(android.R.id.content),"この範囲で保存").isEnabled()));if(!loaded.get())Thread.sleep(100);}
                    check(loaded.get(),"crop screen loads selected image");
                    runOnMainSync(()->{
                        ViewGroup root=cropActivity.findViewById(android.R.id.content);find(root,"90°回転").performClick();
                        Bitmap preview=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(preview));
                        try(FileOutputStream output=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"crop-preview.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,output);}catch(Exception e){throw new AssertionError("crop preview save");}finally{preview.recycle();}
                        find(root,"この範囲で保存").performClick();
                    });
                    for(int attempt=0;attempt<50&&!cropOutput.exists();attempt++)Thread.sleep(100);
                    waitForIdleSync();Bitmap saved=BitmapFactory.decodeFile(cropOutput.getPath());
                    check(saved!=null&&saved.getWidth()==144&&saved.getHeight()==144,"crop screen rotates and saves square PNG");if(saved!=null)saved.recycle();
                    Activity canceled=startActivitySync(cropIntent);waitForIdleSync();runOnMainSync(()->find(canceled.findViewById(android.R.id.content),"キャンセル").performClick());
                    check(cropOutput.exists(),"cancel leaves existing image untouched");
                }finally{cropSource.delete();cropOutput.delete();}
            }
            Intent intent=new Intent(getTargetContext(),MainActivity.class);intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity=startActivitySync(intent);waitForIdleSync();
            runOnMainSync(()->{
                ViewGroup root=activity.findViewById(android.R.id.content);
                Button towel=(Button)find(root,"タオル");Button member=(Button)find(root,"会員証");
                towel.performClick();View towelPane=(View)find(root,"タオルを、さっと。").getParent();
                check(towelPane.getVisibility()==View.VISIBLE,"towel tab opens");
                ImageView image=(ImageView)findDescription(root,"タオルレンタル用QR画像");
                long now=SystemClock.uptimeMillis();float density=activity.getResources().getDisplayMetrics().density;
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,10,100,0);
                MotionEvent up=MotionEvent.obtain(now,now+150,MotionEvent.ACTION_UP,10+140*density,100,0);
                image.dispatchTouchEvent(down);image.dispatchTouchEvent(up);down.recycle();up.recycle();
                check(towelPane.getVisibility()==View.GONE,"right swipe returns to membership");
                towel.performClick();member.performClick();check(towelPane.getVisibility()==View.GONE,"membership tab opens");
                // Draw a sanitized empty state for visual QA. Never capture the user's QR image or the NAS WebView.
                towel.performClick();Drawable original=image.getDrawable();image.setImageDrawable(null);
                root.measure(View.MeasureSpec.makeMeasureSpec(root.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(root.getHeight(),View.MeasureSpec.EXACTLY));root.layout(0,0,root.getWidth(),root.getHeight());
                Bitmap preview=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);root.draw(new Canvas(preview));
                try(FileOutputStream output=new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null),"design-preview.png"))){preview.compress(Bitmap.CompressFormat.PNG,100,output);}catch(Exception e){throw new AssertionError("preview save");}
                finally{preview.recycle();image.setImageDrawable(original);member.performClick();}
            });
        } catch(Throwable e) { code=Activity.RESULT_CANCELED;report.append("FAIL ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append('\n'); }
        finally { if(view!=null)runOnMainSync(()->view.destroy()); }
        result.putString("stream",report.toString()); finish(code,result);
    }
    private void page(String body) throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        runOnMainSync(()->{
            if(view!=null)view.destroy();view=new WebView(getTargetContext());view.getSettings().setJavaScriptEnabled(true);
            view.setWebViewClient(new WebViewClient(){@Override public void onPageFinished(WebView web,String url){ready.countDown();}});
            view.loadDataWithBaseURL(NasAutomation.HOME,"<!doctype html><html><body>"+body+"</body></html>","text/html","UTF-8",null);
        });
        if(!ready.await(15,TimeUnit.SECONDS))throw new AssertionError("test page timeout");
    }
    private String evaluate(String script) throws Exception {
        CountDownLatch ready=new CountDownLatch(1);AtomicReference<String> result=new AtomicReference<>();
        runOnMainSync(()->view.evaluateJavascript(script,value->{result.set(value);ready.countDown();}));
        if(!ready.await(10,TimeUnit.SECONDS))throw new AssertionError("JavaScript timeout");return result.get();
    }
    private View find(View root,String text){
        if(root instanceof TextView && text.contentEquals(((TextView)root).getText()))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;
    }
    private View findDescription(View root,String description){
        if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=findDescription(group.getChildAt(i),description);if(found!=null)return found;}}return null;
    }
}
