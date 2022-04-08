import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

public class Main {

	public static void main(String[] args) {
		BufferedImage img = null;
		int img_num = 1;
		try {
        	img = ImageIO.read(new File("image-" + img_num + ".bmp"));
        	if(img.getWidth() > 800 || img.getHeight() > 800) { // Resize if width or height bigger than 800px
				float scale = (float)img.getWidth() / (float)img.getHeight();
				int newWidth = 0, newHeight = 0;
				
				if(img.getWidth() > img.getHeight()) {
					newWidth = 800;
					newHeight = (int)((float)newWidth / scale);
				}else {
					newHeight = 800;
					newWidth = (int)((float)newHeight * scale);
				}
				
				BufferedImage resizedImg = new BufferedImage(newWidth, newHeight, img.getType());
				Graphics2D g2d = resizedImg.createGraphics();
				g2d.drawImage(img, 0, 0, newWidth, newHeight, null);
				g2d.dispose();
				
				img = resizedImg;
			}
        } catch (IOException e) { e.printStackTrace(); }
		
		// Read vals to array
		int[][] base = convertToValArray(img);
		
		BufferedImage copy = ImageManipulate.trueCopy(img);
		
		ImageManipulate.smoothImage(base);
		ImageManipulate.sharpen(base, 7f);
		writeToFile(img_num + "-0-sharp_smooth.bmp", img, base);
		ImageManipulate.histogramEQ(base);
		writeToFile(img_num + "-1-histeq.bmp", img, base);
		int[][] edges = ImageManipulate.getEdgesFrom(base);
		writeToFile(img_num + "-2-edges.bmp", img, edges);
		int[][] thinned = ImageManipulate.thinEdges(edges);
		writeToFile(img_num + "-3-thin.bmp", img, thinned);
		
		int[][] houghAcculum = Hough.houghTransform(thinned);
		List<HLine> lines = Hough.getLinesFrom(convertToValArray(copy), houghAcculum);
		
		drawLinesOn(copy, lines);
		writeToFile(img_num + "-4-lines.bmp", copy);
	}
	
	private static void writeToFile(String name, BufferedImage image) {
		try {
			File out = new File(name);
			ImageIO.write(image, "bmp", out);
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	private static void writeToFile(String name, BufferedImage image, int[][] imgData) {
		WritableRaster wr = image.getRaster();
		
		for(int j = 0; j < image.getHeight(); ++j) {
			for(int i = 0; i < image.getWidth(); ++i) {
				wr.setSample(i, j, 0, imgData[i][j]);
				wr.setSample(i, j, 1, imgData[i][j]);
				wr.setSample(i, j, 2, imgData[i][j]);
			}
		}
		
		writeToFile(name, image);
	}
	
	private static int[][] convertToValArray(BufferedImage image) {
		int[][] base = new int[image.getWidth()][image.getHeight()];
		WritableRaster wr = image.getRaster();
		
		for(int j = 0; j < image.getHeight(); ++j)
			for(int i = 0; i < image.getWidth(); ++i)
				base[i][j] = wr.getSample(i, j, 0);
				
		return base;
	}
	
	private static void drawLinesOn(BufferedImage image, List<HLine> lines) {
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.RED);
		for(HLine line : lines) g2d.drawLine(line.p1.x, line.p1.y, line.p2.x, line.p2.y);
		
		g2d.dispose();
	}

}
