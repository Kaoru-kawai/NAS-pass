package jp.personal.naspass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textview.MaterialTextView;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import android.graphics.drawable.GradientDrawable;
import android.content.res.ColorStateList;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static final int INK = Color.rgb(23,32,51), MUTED = Color.rgb(88,97,116), PICK_IMAGE = 101, CROP_IMAGE=102;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WebView web, popup;
    private FrameLayout memberPane;
    private LinearLayout towelPane;
    private TextView message, towelHint;
    private ImageView towelImage;
    private Button memberTab, towelTab;
    private MaterialButtonToggleGroup tabGroup;
    private CredentialVault vault;
    private JSONObject credentials;
    private boolean loginAttempted, cardAttempted, resumed;
    private int tab, generation;
    private float startX, startY;
    private File towelFile;
    private Bitmap towelBitmap;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        vault = new CredentialVault(this);
        towelFile = new File(getFilesDir(), "towel-image");
        LinearLayout root = column(); root.setBackgroundColor(Color.rgb(248,249,255));
        root.setPadding(dp(18),dp(12),dp(18),dp(12));
        root.setOnApplyWindowInsetsListener((view,insets)-> {
            view.setPadding(dp(18)+insets.getSystemWindowInsetLeft(),dp(12)+insets.getSystemWindowInsetTop(),dp(18)+insets.getSystemWindowInsetRight(),dp(12)+insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets();
        });
        LinearLayout heading = new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout brand=column();TextView eyebrow=text("SPORTS CLUB",11,MUTED);eyebrow.setLetterSpacing(.16f);brand.addView(eyebrow);
        TextView title = text("NAS Pass",32,INK); title.setTypeface(Typeface.create(getResources().getFont(R.font.nas_sans),Typeface.BOLD));brand.addView(title);
        heading.addView(brand,new LinearLayout.LayoutParams(0,dp(80),1));
        Button settings = outlinedButton("設定"); settings.setOnClickListener(v->settings()); heading.addView(settings); root.addView(heading);
        tabGroup = new MaterialButtonToggleGroup(this);tabGroup.setSingleSelection(true);tabGroup.setSelectionRequired(true);
        memberTab=button("会員証"); towelTab=button("タオル");
        int[][] states=new int[][]{new int[]{android.R.attr.state_checked},new int[]{}};
        for(Button item:new Button[]{memberTab,towelTab}){MaterialButton b=(MaterialButton)item;b.setBackgroundTintList(new ColorStateList(states,new int[]{Color.rgb(33,91,214),Color.rgb(235,239,250)}));b.setTextColor(new ColorStateList(states,new int[]{Color.WHITE,Color.rgb(18,50,114)}));}
        memberTab.setOnClickListener(v->selectTab(0)); towelTab.setOnClickListener(v->selectTab(1));
        memberTab.setId(View.generateViewId());towelTab.setId(View.generateViewId());
        tabGroup.addView(memberTab,new LinearLayout.LayoutParams(0,dp(60),1));tabGroup.addView(towelTab,new LinearLayout.LayoutParams(0,dp(60),1));root.addView(tabGroup);
        tabGroup.addOnButtonCheckedListener((group,id,checked)->{if(checked)selectTab(id==memberTab.getId()?0:1);});
        message = text("",12,MUTED); message.setPadding(0,dp(8),0,dp(8)); root.addView(message);
        FrameLayout deck = new FrameLayout(this); root.addView(deck,new LinearLayout.LayoutParams(-1,0,1));
        memberPane=new FrameLayout(this);memberPane.setBackground(rounded(Color.WHITE,32));memberPane.setClipToOutline(true);deck.addView(memberPane,new FrameLayout.LayoutParams(-1,-1));
        web=createWebView(); memberPane.addView(web,new FrameLayout.LayoutParams(-1,-1));
        towelPane=column(); towelPane.setGravity(Gravity.CENTER); towelPane.setBackground(rounded(Color.rgb(221,231,255),32));
        towelPane.setPadding(dp(20),dp(24),dp(20),dp(20)); deck.addView(towelPane,new FrameLayout.LayoutParams(-1,-1));
        TextView towelTitle=text("タオルを、さっと。",28,INK); towelTitle.setTypeface(Typeface.create(getResources().getFont(R.font.nas_sans),Typeface.BOLD)); towelPane.addView(towelTitle);
        towelHint=text("タオル用QRの写真を1枚選んでください。",13,MUTED); towelHint.setGravity(Gravity.CENTER);towelHint.setPadding(0,dp(8),0,dp(16)); towelPane.addView(towelHint);
        towelImage=new ImageView(this);towelImage.setBackground(rounded(Color.WHITE,24));towelImage.setPadding(dp(16),dp(16),dp(16),dp(16));towelImage.setClipToOutline(true); towelImage.setContentDescription("タオルレンタル用QR画像"); towelImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        towelPane.addView(towelImage,new LinearLayout.LayoutParams(-1,0,1));
        Button pick=button("画像を選ぶ・変更"); pick.setOnClickListener(v->pickImage());LinearLayout.LayoutParams pickParams=new LinearLayout.LayoutParams(-1,dp(64));pickParams.topMargin=dp(16);towelPane.addView(pick,pickParams);
        towelPane.addView(text("画像はこの端末だけに保存されます。",11,MUTED));
        attachSwipe(towelPane); attachSwipe(towelImage);
        TextView hint=text("← スワイプして切り替え →",11,MUTED); hint.setGravity(Gravity.CENTER); hint.setPadding(0,dp(12),0,0); root.addView(hint);
        setContentView(root); root.requestApplyInsets(); selectTab(0); loadTowel(); loadCredentials(); refresh();
        if(credentials==null) message.setText("設定からNASのログイン情報を登録してください。手動ログインも使えます。");
    }

    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private TextView text(String value,int size,int color){TextView t=new MaterialTextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
    private Button button(String value){MaterialButton b=new MaterialButton(this);b.setText(value);b.setAllCaps(false);b.setMinHeight(dp(48));return b;}
    private Button outlinedButton(String value){MaterialButton b=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);b.setText(value);b.setAllCaps(false);return b;}
    private GradientDrawable rounded(int color,int radius){GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(radius));return shape;}
    private void selectTab(int index){tab=index;if(memberPane==null)return;memberPane.setVisibility(index==0?View.VISIBLE:View.GONE);towelPane.setVisibility(index==1?View.VISIBLE:View.GONE);tabGroup.check(index==0?memberTab.getId():towelTab.getId());}
    private void attachSwipe(View view){view.setClickable(true);view.setOnTouchListener((v,event)->{
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){startX=event.getX();startY=event.getY();}
        if(event.getActionMasked()==MotionEvent.ACTION_UP){float dx=event.getX()-startX,dy=event.getY()-startY;if(Math.abs(dx)>dp(70)&&Math.abs(dx)>Math.abs(dy)*1.5){selectTab(dx<0?1:0);return true;}}
        return false;
    });}

    @SuppressWarnings("SetJavaScriptEnabled") private WebView createWebView(){
        WebView view=new WebView(this); view.setBackgroundColor(Color.WHITE);
        WebSettings settings=view.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(true);settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setUseWideViewPort(true);settings.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(view,false);
        view.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                if(!r.isForMainFrame())return false;
                if(NasAutomation.trusted(r.getUrl().toString()))return false;
                message.setText("NAS以外へのリンクはアプリ内では開けません。");return true;
            }
            @Override public void onPageFinished(WebView v,String url){
                CookieManager.getInstance().flush();
                if(v==web && NasAutomation.trusted(url)){int run=++generation; automate(run,0);}
                else if(v==popup)message.setText("NASの会員証を表示しています。タオルへは左スワイプ。");
            }
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError error){h.cancel();message.setText("NASへの安全な接続を確認できません。時間をおいて再試行してください。");}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())message.setText("接続できません。通信状態を確認し、設定から再読み込みしてください。");}
        });
        view.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,Message result){
                if(!NasAutomation.trusted(v.getUrl()) || v!=web)return false;
                closePopup();popup=createWebView();memberPane.addView(popup,new FrameLayout.LayoutParams(-1,-1));
                ((WebView.WebViewTransport)result.obj).setWebView(popup);result.sendToTarget();return true;
            }
            @Override public void onCloseWindow(WebView window){if(window==popup)closePopup();}
        });
        attachSwipe(view);return view;
    }
    private void automate(int run,int tries){
        if(run!=generation || isFinishing() || !NasAutomation.trusted(web.getUrl()))return;
        try{
            web.evaluateJavascript(NasAutomation.script(credentials,!loginAttempted,!cardAttempted),result->{
                if(run!=generation)return;
                if("\"login\"".equals(result)){loginAttempted=true;message.setText("NASにログインしています…");}
                else if("\"card\"".equals(result)){cardAttempted=true;message.setText("会員証を開いています… 表示されない場合はNAS画面の会員証をタップしてください。");return;}
                else if("\"login-required\"".equals(result)){message.setText(loginAttempted?"ログインを確認できません。入力情報を確認するか、NAS画面で続けてください。":"設定からログイン情報を登録するか、NAS画面でログインしてください。");return;}
                if(tries<8)handler.postDelayed(()->automate(run,tries+1),700);
                else if(!cardAttempted)message.setText("NAS画面の会員証をタップしてください。追加認証がある場合は画面で操作してください。");
            });
        }catch(Exception e){message.setText("自動ログインを開始できません。設定からログイン情報を登録し直してください。");}
    }
    private void loadCredentials(){try{credentials=vault.load();}catch(Exception e){credentials=null;message.setText("保存したログイン情報を読み込めません。設定から登録し直してください。");}}
    private void closePopup(){if(popup!=null){WebView old=popup;popup=null;memberPane.removeView(old);old.stopLoading();old.destroy();}}
    private void refresh(){generation++;handler.removeCallbacksAndMessages(null);loginAttempted=false;cardAttempted=false;closePopup();web.stopLoading();message.setText("NASに接続しています…");web.loadUrl(NasAutomation.HOME);}
    private void settings(){
        new MaterialAlertDialogBuilder(this).setTitle("NAS Pass 設定").setItems(new String[]{"自動ログインを設定","会員証を再読み込み","タオル画像を選ぶ","タオル画像を削除","ログイン情報を削除"},(dialog,which)->{
            if(which==0)editCredentials();
            if(which==1){selectTab(0);refresh();}
            if(which==2)pickImage();
            if(which==3)new MaterialAlertDialogBuilder(this).setMessage("保存したタオル画像を削除しますか？").setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,w)->{if(towelFile.exists()&&!towelFile.delete()){message.setText("画像を削除できませんでした。");return;}loadTowel();message.setText("タオル画像を削除しました。");}).show();
            if(which==4)new MaterialAlertDialogBuilder(this).setMessage("ログイン情報とNASのログイン状態をこの端末から削除しますか？").setNegativeButton("キャンセル",null).setPositiveButton("削除",(d,w)->{
                try{vault.clear();credentials=null;generation++;handler.removeCallbacksAndMessages(null);web.stopLoading();closePopup();android.webkit.WebStorage.getInstance().deleteAllData();web.clearCache(true);web.clearHistory();CookieManager.getInstance().removeAllCookies(done->{CookieManager.getInstance().flush();refresh();});}catch(Exception e){message.setText("ログイン情報を削除できませんでした。");}
            }).show();
        }).setNegativeButton("閉じる",null).show();
    }
    private void editCredentials(){
        LinearLayout form=column();form.setPadding(dp(24),dp(8),dp(24),0);
        TextView notice=text("この端末に暗号化して保存します。自動ログイン時にスポーツクラブNAS公式サイトへ送信します。",13,MUTED);form.addView(notice);
        TextInputLayout memberLayout=new TextInputLayout(this);memberLayout.setHint("会員番号（半角）");memberLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
        EditText member=new TextInputEditText(memberLayout.getContext());member.setSingleLine(true);member.setInputType(InputType.TYPE_CLASS_TEXT);memberLayout.addView(member);form.addView(memberLayout);
        TextInputLayout passwordLayout=new TextInputLayout(this);passwordLayout.setHint("パスワード");passwordLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        EditText password=new TextInputEditText(passwordLayout.getContext());password.setSingleLine(true);password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);passwordLayout.addView(password);form.addView(passwordLayout);
        member.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);password.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        if(credentials!=null)member.setText(credentials.optString("member"));
        AlertDialog dialog=new MaterialAlertDialogBuilder(this).setTitle("自動ログイン").setView(form).setNegativeButton("キャンセル",null).setPositiveButton("保存してログイン",null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            String number=member.getText().toString().trim(),pass=password.getText().toString();
            if(number.isEmpty()||pass.isEmpty()){password.setError("会員番号とパスワードを入力してください。");return;}
            try{vault.save(number,pass);loadCredentials();password.setText("");dialog.dismiss();selectTab(0);refresh();}catch(Exception e){password.setError("端末に保存できませんでした。");}
        }));dialog.show();
    }
    private void pickImage(){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,PICK_IMAGE);}
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CROP_IMAGE){
            File cropped=new File(getFilesDir(),"crop-output");
            if(result==RESULT_OK){if(cropped.renameTo(towelFile)){loadTowel();selectTab(1);message.setText("正方形に切り抜いたQR画像を保存しました。");}else message.setText("画像を保存できませんでした。前の画像は保持しています。");}
            new File(getFilesDir(),"crop-source").delete();cropped.delete();return;
        }
        if(request!=PICK_IMAGE || result!=RESULT_OK || data==null || data.getData()==null)return;
        Uri uri=data.getData();File temp=new File(getFilesDir(),"crop-source");
        try{
            try(InputStream input=getContentResolver().openInputStream(uri);FileOutputStream output=new FileOutputStream(temp)){
                if(input==null)throw new IllegalArgumentException();byte[] buffer=new byte[8192];int length,total=0;
                while((length=input.read(buffer))!=-1){total+=length;if(total>15*1024*1024)throw new IllegalArgumentException();output.write(buffer,0,length);}
            }
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(temp.getPath(),options);
            if(options.outWidth<=0||options.outHeight<=0)throw new IllegalArgumentException();
            startActivityForResult(new Intent(this,CropActivity.class),CROP_IMAGE);
        }catch(Exception e){temp.delete();message.setText("画像を読み込めませんでした。15MB以下のJPEG・PNGなどの画像を選んでください。");}
    }
    private void loadTowel(){
        towelImage.setImageDrawable(null);if(towelBitmap!=null){towelBitmap.recycle();towelBitmap=null;}
        if(towelFile.exists()){
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(towelFile.getPath(),options);
            options.inSampleSize=1;while(options.outWidth/options.inSampleSize>4096||options.outHeight/options.inSampleSize>4096)options.inSampleSize*=2;
            options.inJustDecodeBounds=false;towelBitmap=BitmapFactory.decodeFile(towelFile.getPath(),options);towelImage.setImageBitmap(towelBitmap);
        }
        towelHint.setText(towelBitmap==null?"タオル用QRの写真を1枚選んでください。":"QRをレンタル機の読み取り部にかざしてください。");
    }
    @Override protected void onResume(){super.onResume();if(web!=null){web.onResume();if(resumed && tab==0 && credentials!=null)refresh();resumed=true;}}
    @Override protected void onPause(){if(web!=null)web.onPause();super.onPause();}
    @Override public void onBackPressed(){if(tab==1){selectTab(0);return;}if(popup!=null){closePopup();return;}if(web.canGoBack()){web.goBack();return;}super.onBackPressed();}
    @Override protected void onDestroy(){generation++;handler.removeCallbacksAndMessages(null);closePopup();if(web!=null)web.destroy();if(towelBitmap!=null)towelBitmap.recycle();super.onDestroy();}
}

