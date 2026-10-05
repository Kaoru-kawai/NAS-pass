package jp.personal.naspass;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.exifinterface.media.ExifInterface;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CropActivity extends AppCompatActivity {
    private SquareCropView crop;
    private Bitmap bitmap;
    private TextView status;
    private MaterialButton save,rotate;
    private Slider size;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private boolean busy;
    private int rotationDegrees;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(248,249,255));root.setPadding(dp(20),dp(12),dp(20),dp(12));
        root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(dp(20)+i.getSystemWindowInsetLeft(),dp(12)+i.getSystemWindowInsetTop(),dp(20)+i.getSystemWindowInsetRight(),dp(12)+i.getSystemWindowInsetBottom());return i.consumeSystemWindowInsets();});
        TextView title=new TextView(this);title.setText("QRを切り抜く");title.setTextSize(28);title.setTypeface(null,Typeface.BOLD);root.addView(title);
        status=new TextView(this);status.setText("枠を移動・四隅でサイズ調整。QRの周囲に白い余白を残してください。");status.setPadding(0,dp(8),0,dp(16));root.addView(status);
        crop=new SquareCropView(this);root.addView(crop,new LinearLayout.LayoutParams(-1,0,1));
        size=new Slider(this);size.setValueFrom(.1f);size.setValueTo(1f);size.setValue(.8f);size.setContentDescription("切り抜き枠のサイズ");size.addOnChangeListener((slider,value,user)->{if(user)crop.resize(value);});root.addView(size);
        LinearLayout adjust=new LinearLayout(this);adjust.setGravity(Gravity.CENTER);
        for(String direction:new String[]{"←","↑","↓","→"}){MaterialButton button=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);button.setText(direction);button.setContentDescription("切り抜き枠を"+direction+"へ移動");button.setOnClickListener(v->crop.move(direction.equals("←")?-dp(12):direction.equals("→")?dp(12):0,direction.equals("↑")?-dp(12):direction.equals("↓")?dp(12):0));adjust.addView(button,new LinearLayout.LayoutParams(0,dp(48),1));}root.addView(adjust);
        rotate=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);rotate.setText("90°回転");rotate.setEnabled(false);rotate.setOnClickListener(v->{Matrix m=new Matrix();m.postRotate(90);Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),m,true);if(rotated!=bitmap)bitmap.recycle();bitmap=rotated;rotationDegrees=(rotationDegrees+90)%360;crop.setBitmap(bitmap);size.setValue(.8f);});root.addView(rotate,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout actions=new LinearLayout(this);MaterialButton cancel=new MaterialButton(this,null,com.google.android.material.R.attr.materialButtonOutlinedStyle);cancel.setText("キャンセル");cancel.setOnClickListener(v->{if(!busy){setResult(Activity.RESULT_CANCELED);finish();}});actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(64),1));
        save=new MaterialButton(this);save.setText("この範囲で保存");save.setEnabled(false);save.setOnClickListener(v->save());actions.addView(save,new LinearLayout.LayoutParams(0,dp(64),1));root.addView(actions);setContentView(root);root.requestApplyInsets();
        float[] selection=state==null?null:state.getFloatArray("selection");int rotation=state==null?0:state.getInt("rotation",0);
        rotationDegrees=rotation;
        worker.execute(()->{try{Bitmap decoded=decode(new File(getFilesDir(),"crop-source"));if(rotation!=0){Matrix m=new Matrix();m.postRotate(rotation);Bitmap rotated=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),m,true);if(rotated!=decoded)decoded.recycle();decoded=rotated;}Bitmap ready=decoded;runOnUiThread(()->{if(isDestroyed()){ready.recycle();return;}bitmap=ready;crop.setBitmap(bitmap);if(selection!=null)crop.restoreSelection(selection);save.setEnabled(true);rotate.setEnabled(true);});}catch(Exception|OutOfMemoryError e){runOnUiThread(()->status.setText("画像を読み込めません。別の画像を選んでください。"));}});
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    static Bitmap decode(File file)throws IOException{
        BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeFile(file.getPath(),options);if(options.outWidth<=0||options.outHeight<=0)throw new IOException();
        options.inSampleSize=1;while(options.outWidth/options.inSampleSize>3072||options.outHeight/options.inSampleSize>3072)options.inSampleSize*=2;options.inJustDecodeBounds=false;
        Bitmap decoded=BitmapFactory.decodeFile(file.getPath(),options);if(decoded==null)throw new IOException();
        int orientation=new ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);Matrix transform=new Matrix();
        switch(orientation){case 2:transform.setScale(-1,1);break;case 3:transform.setRotate(180);break;case 4:transform.setScale(1,-1);break;case 5:transform.setRotate(90);transform.postScale(-1,1);break;case 6:transform.setRotate(90);break;case 7:transform.setRotate(-90);transform.postScale(-1,1);break;case 8:transform.setRotate(-90);break;}
        Bitmap oriented=Bitmap.createBitmap(decoded,0,0,decoded.getWidth(),decoded.getHeight(),transform,true);if(oriented!=decoded)decoded.recycle();return oriented;
    }
    private void save(){
        busy=true;save.setEnabled(false);rotate.setEnabled(false);size.setEnabled(false);crop.setEnabled(false);status.setText("保存しています…");
        final Bitmap output;try{output=crop.export();}catch(Exception|OutOfMemoryError e){busy=false;save.setEnabled(true);status.setText("切り抜けませんでした。範囲を調整してください。");return;}
        worker.execute(()->{boolean ok=false;try(FileOutputStream stream=new FileOutputStream(new File(getFilesDir(),"crop-output"))){ok=output.compress(Bitmap.CompressFormat.PNG,100,stream);stream.getFD().sync();}catch(IOException ignored){}finally{output.recycle();}boolean saved=ok;runOnUiThread(()->{busy=false;if(saved){setResult(Activity.RESULT_OK);finish();}else{save.setEnabled(true);rotate.setEnabled(true);size.setEnabled(true);crop.setEnabled(true);status.setText("保存できませんでした。端末の空き容量を確認してください。");}});});
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putFloatArray("selection",crop.selection());state.putInt("rotation",rotationDegrees);}
    @Override public void onBackPressed(){if(!busy)super.onBackPressed();}
    @Override protected void onDestroy(){worker.shutdown();if(bitmap!=null)bitmap.recycle();super.onDestroy();}
}
