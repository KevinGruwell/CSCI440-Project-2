import java.util.LinkedList;
import java.util.List;

public class Hough {

	public static int[][] houghTransform(int[][] imgData) { 
		int imgWidth = imgData.length;
		int imgHeight = imgData[0].length;
		Coord center = new Coord(imgWidth / 2, imgHeight / 2);
		
		int thetas = 180;
		int accuHeight = (int)(Math.max(imgWidth, imgHeight) * Math.sqrt(2)) / 2;
		int doubleHeight = accuHeight * 2; // accounts for negatives
		
		int[][] accumulator = new int[thetas][doubleHeight];
		
		for(int j = 0; j < imgHeight; ++j) {
			for(int i = 0; i < imgWidth; ++i) {
				if(imgData[i][j] == 0) { // every edge
					for(int deg = 0; deg < 180; ++deg) { // every possible angle of straight line
						double theta = ((double)deg * (Math.PI / 180));
						
						int d = (int)(((double)(i - center.x) * Math.cos(theta)) + ((double)(j - center.y) * Math.sin(theta)));
						accumulator[deg][d + accuHeight]++;
					}
				}
			}
		}
		
		return accumulator;
	}
	
	public static List<HLine> getLinesFrom(int[][] imgData, int[][] transform) {
		int imgWidth = imgData.length;
		int imgHeight = imgData[0].length;
		Coord center = new Coord(imgWidth / 2, imgHeight / 2);
		
		int thetas = 180;
		int accuHeight = (int)(Math.max(imgWidth, imgHeight) * Math.sqrt(2)) / 2;
		int doubleHeight = accuHeight * 2;
		
		int threshold = 70; // adjust for more/less lines
		
		List<HLine> lines = new LinkedList<HLine>();

		for(int d = 0; d < doubleHeight; ++d) {
			for(int deg = 0; deg < thetas; ++deg) {
				if(transform[deg][d] >= threshold) {
					int max = transform[deg][d];
					
					// checks for bigger elements nearby, if found then ignore this one
					for(int y = -2; y < 3; ++y) {
						for(int x = -2; x < 3; ++x) {
							if((deg + x) >= 0 && (deg + x) < thetas && (d + y) >= 0 && (d + y) < doubleHeight) {
								if(transform[deg + x][d + y] > max) {
									max = transform[deg + x][d + y];
									
									// Force break
									x = 3;
									y = 3;
								}
							}
						}
					}
					if(max > transform[deg][d]) continue;
					
					int theta = (int)(deg * (Math.PI / 180));
					int x1 = 0, y1 = 0, x2 = 0, y2 = 0;
					
					if(deg >= 45 && deg <= 135) {
						x2 = imgWidth;
						y1 = (int) (((d - accuHeight) - ((x1 - center.x) * Math.cos(theta))) / Math.sin(theta) + center.y);
						y2 = (int) (((d - accuHeight) - ((x2 - center.x) * Math.cos(theta))) / Math.sin(theta) + center.y);
					}else {
						y2 = imgHeight;
						x1 = (int) (((d - accuHeight) - ((y1 - center.y) * Math.sin(theta))) / Math.cos(theta) + center.x);
						x2 = (int) (((d - accuHeight) - ((y2 - center.y) * Math.sin(theta))) / Math.cos(theta) + center.x);
					}
	 
					lines.add(new HLine(new Coord(x1, y1), new Coord(x2, y2)));
				}
			}
		}

	    return lines;
	}
}