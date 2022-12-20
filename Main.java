import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;

public class Main{
  public static Color getColor(BufferedImage image, int x, int y){
    int color = image.getRGB(x, y);
    return new Color((color >> 16) & 0xff, (color >> 8) & 0xff, (color) & 0xff);
  }
  public static Color getAverageColor(BufferedImage image){
    int rt=0,gt=0,bt=0;
    for (int x = 0; x < image.getWidth(); x++) {
        for (int y = 0; y < image.getHeight(); y++) {
            int color = image.getRGB(x, y);
            rt += (color >> 16) & 0xff;
            gt += (color >> 8) & 0xff;
            bt += (color) & 0xff;
        }
    }
    rt/=(image.getWidth()*image.getHeight());
    gt/=(image.getWidth()*image.getHeight());
    bt/=(image.getWidth()*image.getHeight());
    return new Color(rt,gt,bt);
  }

  public static Integer quantizedHue(Color color){
    return ((int) (Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0] * 360 * 6));
  }
  public static float saturation(Color color){
    return (Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[1]);
  }

  public static float brightness(Color color){
    return (Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[2]);
  }

  public static Map<Integer,BufferedImage> mapImagesByHue(File[] listOfFiles)throws IOException{
    Map<Integer,BufferedImage> imagesByHue = new TreeMap<>();
    for (File file : listOfFiles) {
      if (file.isFile() && file.getName().endsWith(".png")) {
        BufferedImage image = ImageIO.read(file);
        Color imageColor = getAverageColor(image);
        Integer hue = quantizedHue(imageColor);
        imagesByHue.put(hue,image);
      }
    }
    return imagesByHue;
  }
  public static int applyBrightness(int c, float brightness){
      Double v = (double)c;
      if(brightness>=0.5){
        v = (255 - v)*(brightness-0.5)*2 + v;
      } else {
        v = (v)*(brightness)*2 ;
      }
      return (int) v.doubleValue();
  }
  
  public static BufferedImage applyBrightnessOnImage(BufferedImage inputImage, float brightness) {
    BufferedImage outputImage = new BufferedImage(inputImage.getWidth(), inputImage.getHeight(), BufferedImage.TYPE_INT_RGB);

    for (int y = 0; y < inputImage.getHeight(); y++) {
      for (int x = 0; x < inputImage.getWidth(); x++) {
        int color = inputImage.getRGB(x, y);
        int r = (color >> 16) & 0xff;
        int g = (color >> 8) & 0xff;
        int b = (color) & 0xff;
        r = applyBrightness(r,brightness);
        g = applyBrightness(g,brightness);
        b = applyBrightness(b,brightness);
        color = (r << 16) | (g << 8) | b;
        outputImage.setRGB(x, y, color);
      }
    }
    return outputImage;
  }

  public static void main(String[] args) throws IOException{
    File[] listOfFiles = new File("assets").listFiles();
    if(listOfFiles == null)
      return;
    Map<Integer,BufferedImage> imagesByHue = mapImagesByHue(listOfFiles);
    
    BufferedImage poster = ImageIO.read(new File("poster.png"));
    BufferedImage newPoster = new BufferedImage(poster.getWidth()*4, poster.getHeight()*4, BufferedImage.TYPE_INT_RGB);
    
    Graphics2D g2d = newPoster.createGraphics();

    g2d.setColor(Color.black);
    g2d.fillRect(0, 0, newPoster.getWidth(), newPoster.getHeight());
    int npixels = poster.getWidth()*poster.getHeight();
    Random random = new Random();
    for (int i = 0; i < npixels / 5 ; i+=1) {
        int x = random.nextInt(poster.getWidth()), y= random.nextInt(poster.getHeight());
        Color pixelColor = getColor(poster,x,y);
        Integer hue = quantizedHue(pixelColor);
        BufferedImage matchedImage = imagesByHue.get(hue);
        if(matchedImage == null)
          matchedImage = imagesByHue.get(hue-1);
        if(matchedImage == null)
          matchedImage = imagesByHue.get(hue+1);
        if(matchedImage == null)
              continue;

        float pixelBrightness = brightness(pixelColor);
        matchedImage = applyBrightnessOnImage(matchedImage,pixelBrightness);

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pixelBrightness));

        g2d.drawImage(matchedImage, x*4, y*4, null);
    }  

    g2d.dispose();

    // Save as PNG
    File file = new File("new_poster.png");
    ImageIO.write(newPoster, "png", file);


  } 
}