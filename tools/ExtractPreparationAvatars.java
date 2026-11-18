import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Offline asset preparation only; runtime recognition executes on the server. */
public class ExtractPreparationAvatars {
    public static void main(String[] args) throws Exception {
        if (args.length!=6) throw new IllegalArgumentException("用法：java tools/ExtractPreparationAvatars.java 截图 输出目录 contentX contentY contentWidth contentHeight");
        var image=ImageIO.read(Path.of(args[0]).toFile());
        double x=Double.parseDouble(args[2]),y=Double.parseDouble(args[3]),w=Double.parseDouble(args[4]),h=Double.parseDouble(args[5]);
        if (x<0||y<0||w<=0||h<=0||x+w>1||y+h>1) throw new IllegalArgumentException("内容区域超出图片");
        var output=Path.of(args[1]);Files.createDirectories(output);
        double[][] panels={{.030,.430,.074,.117,.061,.112},{.757,.557,.075,.120,.061,.112}};
        var sheet=new BufferedImage(6*150,2*170,BufferedImage.TYPE_INT_RGB);
        var g=sheet.createGraphics();g.setColor(Color.WHITE);g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        for(int side=0;side<2;side++)for(int slot=0;slot<6;slot++){
            var p=panels[side];
            int left=(int)Math.round((x+(p[0]+slot%3*p[2])*w)*image.getWidth());
            int top=(int)Math.round((y+(p[1]+slot/3*p[3])*h)*image.getHeight());
            int width=(int)Math.round(p[4]*w*image.getWidth()),height=(int)Math.round(p[5]*h*image.getHeight());
            var avatar=image.getSubimage(left,top,width,height);
            String key=(side==0?"self":"opponent")+"-"+(slot+1);
            ImageIO.write(avatar,"png",output.resolve(key+".png").toFile());
            g.drawImage(avatar,slot*150+15,side*170+10,120,120,null);
            g.setColor(Color.BLACK);g.drawString(key+(slot==0?" (lead)":""),slot*150+15,side*170+155);
        }
        g.dispose();ImageIO.write(sheet,"png",output.resolve("contact-sheet.png").toFile());
        System.out.println("已裁出 12 张待标注头像："+output.toAbsolutePath());
    }
}
