import java.awt.*;
import java.awt.font.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import java.util.Locale;

/** Generates identical NAS letterforms for the adaptive icon and Play artwork. */
public class GenerateIcon {
    public static void main(String[] args)throws Exception{
        Path root=Path.of(args[0]);Path assets=root.resolve("output/icons");Files.createDirectories(assets);
        Font font=new Font("SansSerif",Font.BOLD,100);
        Shape glyph=font.createGlyphVector(new FontRenderContext(null,true,true),"NAS").getOutline();Rectangle2D bounds=glyph.getBounds2D();
        AffineTransform transform=new AffineTransform();transform.translate(54,54);double scale=52/bounds.getWidth();transform.scale(scale,scale);transform.translate(-bounds.getCenterX(),-bounds.getCenterY());
        Shape letters=transform.createTransformedShape(glyph);String path=path(letters);
        String vector="<vector xmlns:android=\"http://schemas.android.com/apk/res/android\" android:width=\"108dp\" android:height=\"108dp\" android:viewportWidth=\"108\" android:viewportHeight=\"108\"><path android:fillColor=\"#FFFFFF\" android:pathData=\""+path+"\"/></vector>";
        Path res=root.resolve("android/app/src/main/res");Files.writeString(res.resolve("drawable/ic_nas_foreground.xml"),vector);
        Files.writeString(assets.resolve("nas-icon.svg"),"<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 108 108\"><rect width=\"108\" height=\"108\" fill=\"#215BD6\"/><path fill=\"white\" d=\""+path+"\"/></svg>");
        BufferedImage image=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(new Color(0x215BD6));g.fillRect(0,0,512,512);
        // Play artwork can use more of the square than the launcher safe area.
        g.translate(256,256);g.scale(340/bounds.getWidth(),340/bounds.getWidth());g.translate(-bounds.getCenterX(),-bounds.getCenterY());g.setColor(Color.WHITE);g.fill(glyph);g.dispose();
        ImageWriter writer=ImageIO.getImageWritersByFormatName("png").next();ImageWriteParam param=writer.getDefaultWriteParam();IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image),param);
        IIOMetadataNode tree=new IIOMetadataNode("javax_imageio_png_1.0");IIOMetadataNode srgb=new IIOMetadataNode("sRGB");srgb.setAttribute("renderingIntent","Perceptual");tree.appendChild(srgb);metadata.mergeTree("javax_imageio_png_1.0",tree);
        try(ImageOutputStream output=ImageIO.createImageOutputStream(assets.resolve("nas-play-icon-512.png").toFile())){writer.setOutput(output);writer.write(null,new IIOImage(image,null,metadata),param);}writer.dispose();
        System.out.println("Generated adaptive NAS vector and Play PNG: "+Files.size(assets.resolve("nas-play-icon-512.png"))+" bytes");
    }
    static String path(Shape shape){StringBuilder out=new StringBuilder();double[] values=new double[6];for(PathIterator it=shape.getPathIterator(null);!it.isDone();it.next()){int type=it.currentSegment(values);out.append("MLQCZ".charAt(type));int count=type==0||type==1?2:type==2?4:type==3?6:0;for(int i=0;i<count;i++){if(i>0)out.append(',');out.append(String.format(Locale.ROOT,"%.1f",values[i]));}}return out.toString();}
}
