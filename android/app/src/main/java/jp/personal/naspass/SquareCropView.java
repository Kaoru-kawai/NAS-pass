package jp.personal.naspass;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/** A movable square selection. Only the selected source pixels are exported. */
public final class SquareCropView extends View {
    private Bitmap bitmap;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF image=new RectF(), crop=new RectF();
    private float lastX,lastY,anchorX,anchorY;
    private int corner=-1;
    private boolean moving;
    private float[] restored;
    public SquareCropView(Context context){super(context);setLayerType(LAYER_TYPE_SOFTWARE,null);setContentDescription("正方形の切り抜き枠。枠をドラッグで移動、四隅をドラッグでサイズ調整。");setFocusable(true);}
    public void setBitmap(Bitmap value){bitmap=value;crop.setEmpty();layoutImage();invalidate();}
    public float[] selection(){if(image.width()<=0)return null;return new float[]{(crop.left-image.left)/image.width(),(crop.top-image.top)/image.height(),crop.width()/image.width()};}
    public void restoreSelection(float[] selection){restored=selection;layoutImage();invalidate();}
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH){float[] saved=selection();if(saved!=null)restored=saved;layoutImage();}
    private void layoutImage(){
        if(bitmap==null||getWidth()==0||getHeight()==0)return;
        float padding=24*getResources().getDisplayMetrics().density;
        float scale=Math.min(Math.max(1,getWidth()-padding*2)/bitmap.getWidth(),Math.max(1,getHeight()-padding*2)/bitmap.getHeight());
        float w=bitmap.getWidth()*scale,h=bitmap.getHeight()*scale;
        image.set((getWidth()-w)/2,(getHeight()-h)/2,(getWidth()+w)/2,(getHeight()+h)/2);
        float side=Math.min(w,h)*.8f;
        if(restored!=null&&restored.length==3){side=Math.min(Math.min(w,h),Math.max(1,restored[2]*w));float left=image.left+restored[0]*w,top=image.top+restored[1]*h;left=Math.max(image.left,Math.min(left,image.right-side));top=Math.max(image.top,Math.min(top,image.bottom-side));crop.set(left,top,left+side,top+side);restored=null;}
        else crop.set(image.centerX()-side/2,image.centerY()-side/2,image.centerX()+side/2,image.centerY()+side/2);
    }
    @Override protected void onDraw(Canvas canvas){
        canvas.drawColor(Color.rgb(17,24,40));if(bitmap==null)return;
        paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.FILL);canvas.drawBitmap(bitmap,null,image,paint);
        paint.setColor(0xB3101728);
        canvas.drawRect(0,0,getWidth(),crop.top,paint);canvas.drawRect(0,crop.bottom,getWidth(),getHeight(),paint);
        canvas.drawRect(0,crop.top,crop.left,crop.bottom,paint);canvas.drawRect(crop.right,crop.top,getWidth(),crop.bottom,paint);
        paint.setColor(Color.WHITE);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*getResources().getDisplayMetrics().density);canvas.drawRect(crop,paint);
        paint.setColor(0x66FFFFFF);paint.setStrokeWidth(1);
        for(int i=1;i<3;i++){float x=crop.left+crop.width()*i/3,y=crop.top+crop.height()*i/3;canvas.drawLine(x,crop.top,x,crop.bottom,paint);canvas.drawLine(crop.left,y,crop.right,y,paint);}
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);float radius=7*getResources().getDisplayMetrics().density;
        canvas.drawCircle(crop.left,crop.top,radius,paint);canvas.drawCircle(crop.right,crop.top,radius,paint);canvas.drawCircle(crop.left,crop.bottom,radius,paint);canvas.drawCircle(crop.right,crop.bottom,radius,paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(bitmap==null||!isEnabled())return false;float x=event.getX(),y=event.getY();float reach=32*getResources().getDisplayMetrics().density;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN){
            corner=-1;float[] xs={crop.left,crop.right,crop.left,crop.right},ys={crop.top,crop.top,crop.bottom,crop.bottom};
            for(int i=0;i<4;i++)if(Math.hypot(x-xs[i],y-ys[i])<reach){corner=i;break;}
            moving=corner==-1&&crop.contains(x,y);if(!moving&&corner==-1)return false;
            lastX=x;lastY=y;if(corner>=0){anchorX=xs[3-corner];anchorY=ys[3-corner];}getParent().requestDisallowInterceptTouchEvent(true);return true;
        }
        if(event.getActionMasked()==MotionEvent.ACTION_MOVE){
            if(moving){float dx=Math.max(image.left-crop.left,Math.min(x-lastX,image.right-crop.right)),dy=Math.max(image.top-crop.top,Math.min(y-lastY,image.bottom-crop.bottom));crop.offset(dx,dy);}
            else if(corner>=0){boolean left=corner==0||corner==2,top=corner==0||corner==1;float desired=((left?anchorX-x:x-anchorX)+(top?anchorY-y:y-anchorY))/2;
                float max=Math.min(left?anchorX-image.left:image.right-anchorX,top?anchorY-image.top:image.bottom-anchorY);
                float side=Math.min(max,Math.max(Math.min(48*getResources().getDisplayMetrics().density,max),desired));crop.set(left?anchorX-side:anchorX,top?anchorY-side:anchorY,left?anchorX:anchorX+side,top?anchorY:anchorY+side);}
            lastX=x;lastY=y;invalidate();return true;
        }
        if(event.getActionMasked()==MotionEvent.ACTION_UP||event.getActionMasked()==MotionEvent.ACTION_CANCEL){moving=false;corner=-1;performClick();return true;}return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    public void resize(float fraction){float side=Math.min(image.width(),image.height())*Math.max(.1f,Math.min(1f,fraction));float left=Math.max(image.left,Math.min(crop.centerX()-side/2,image.right-side)),top=Math.max(image.top,Math.min(crop.centerY()-side/2,image.bottom-side));crop.set(left,top,left+side,top+side);invalidate();}
    public void move(float dx,float dy){crop.offset(Math.max(image.left-crop.left,Math.min(dx,image.right-crop.right)),Math.max(image.top-crop.top,Math.min(dy,image.bottom-crop.bottom)));invalidate();}
    public Bitmap export(){
        if(bitmap==null||image.width()<=0)throw new IllegalStateException("画像がありません");
        float scale=bitmap.getWidth()/image.width();int size=Math.max(1,Math.min(Math.min(bitmap.getWidth(),bitmap.getHeight()),Math.round(crop.width()*scale)));
        int x=Math.max(0,Math.min(bitmap.getWidth()-size,Math.round((crop.left-image.left)*scale))),y=Math.max(0,Math.min(bitmap.getHeight()-size,Math.round((crop.top-image.top)*scale)));
        // A copy owns its pixels even when the selection includes the whole source image.
        Bitmap selected=Bitmap.createBitmap(bitmap,x,y,size,size);Bitmap output=selected.copy(Bitmap.Config.ARGB_8888,false);if(selected!=bitmap)selected.recycle();return output;
    }
}
