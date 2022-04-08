import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;

public class ImageManipulate {

	public static void sharpen(int[][] imgData, float factor) {
		float sharpness = 1f + factor;
		
		for(int j = 0; j < imgData[0].length; ++j) {
			for(int i = 0; i < imgData.length; ++i) {
				int res = Math.round((int) (127 + sharpness * (imgData[i][j] - 127)));
				if(res <= 0) imgData[i][j] = 0;
				else if(res >= 255) imgData[i][j] = 255;
				else imgData[i][j] = res;
			}
		}
	}
	
	public static void histogramEQ(int[][] imgData) {
		int[] histograms = new int[256];
		
		int imgWidth = imgData.length;
		int imgHeight = imgData[0].length;
		
		// Init all to 0
		for(int i = 0; i < histograms.length; ++i) histograms[i] = 0;
		
		// Build histogram based on grayscale values
		for(int j = 0; j < imgHeight; ++j)
			for(int i = 0; i < imgWidth; ++i)
				histograms[imgData[i][j]] += 1;
		
		// Build cumulative histogram
		for(int i = 1; i < histograms.length; ++i) histograms[i] += histograms[i - 1];
		
		// Normalize cumulative histogram
		for(int i = 0; i < histograms.length; ++i) histograms[i] = (histograms[i] * 255) / (imgWidth * imgHeight);
		
		// Write equalized values to image
		for(int j = 0; j < imgHeight; ++j)
			for(int i = 0; i < imgWidth; ++i)
				imgData[i][j] = histograms[imgData[i][j]];
	}
	
	// Smoothes pixels in a 3x3 area
	public static void smoothImage(int[][] imgData) {
		int imgWidth = imgData.length;
		int imgHeight = imgData[0].length;
		
		int[][] base = new int[imgWidth][imgHeight];
		for(int j = 0; j < imgHeight; ++j)
			for(int i = 0; i < imgWidth; ++i)
				base[i][j] = imgData[i][j];
		
		for(int j = 1; j < imgHeight - 1; ++j) {
			for(int i = 1; i < imgWidth - 1; ++i) {
				float acculum = 0f;
				
				for(int y = -1; y < 2; ++y)
					for(int x = -1; x < 2; ++x)
						acculum += base[i + x][j + y];
				
				imgData[i][j] = (int)Math.floor(acculum / 9);
			}
		}
		
		
	}

	private static int[][] sobelX = new int[][] {{-1, 0, 1},
		                                         {-2, 0, 2},
		                                         {-1, 0, 1}};

	private static int[][] sobelY = new int[][] {{1, 2, 1},
                                                 {0, 0, 0},
                                                 {-1, -2, -1}};
        
    public static int[][] getEdgesFrom(int[][] imgData) {
    	int[][] xTrans = ImageManipulate.sobelX;
    	int[][] yTrans = ImageManipulate.sobelY;
    	
    	int imgWidth = imgData.length;
		int imgHeight = imgData[0].length;
    	
		int[][] edges = new int[imgWidth][imgHeight];
		for(int j = 1; j < imgHeight - 1; ++j)
			for(int i = 1; i < imgWidth - 1; ++i)
				edges[i][j] = 0;
		
		for(int j = 1; j < imgHeight - 1; ++j) {
			for(int i = 1; i < imgWidth - 1; ++i) {
				int sumX = 0, sumY = 0;
				
				for(int y = -1; y < xTrans[0].length - 1; ++y)
					for(int x = -1; x < xTrans.length - 1; ++x)
						sumX += (xTrans[x + 1][y + 1] * imgData[i + x][j + y]);
				
				for(int y = -1; y < yTrans[0].length - 1; ++y)
					for(int x = -1; x < yTrans.length - 1; ++x)
						sumY += (yTrans[x + 1][y + 1] * imgData[i + x][j + y]);
				
				edges[i][j] = (int) Math.floor(Math.sqrt(Math.pow(sumX, 2) + Math.pow(sumY, 2)));
			}
		}
		
		int threshold = 100; // Adjust for better/worse edges
		for(int j = 1; j < imgHeight - 1; ++j)
			for(int i = 1; i < imgWidth - 1; ++i)
				edges[i][j] = (edges[i][j] > threshold) ? 0 : 255;
		
		// Noise killing
		int nk_thresh = 2;
		for(int j = 1; j < imgHeight - 1; ++j) {
			for(int i = 1; i < imgWidth - 1; ++i) {
				int count = 0;
				
				for(int y = -1; y < 2; ++y) {
					for(int x = -1; x < 2; ++x) {
						if(x == 0 && y == 0) continue;
						if(edges[i + x][j + y] == 0) ++count;
					}
				}
				
				if(count < nk_thresh) {
					edges[i][j] = 255;
				}
			}
		}
		
		return edges;
    }

    private static final int[][][] contourPoints = new int[][][] { {{-1,-1,-1},{-1,1,-1},{-1,0,-1}}, 
    	                                                           {{-1,0,-1},{-1,1,-1},{-1,-1,-1}}, 
    	                                                           {{-1,-1,-1},{0,1,-1},{-1,-1,-1}}, 
    	                                                           {{-1,-1,-1},{-1,1,0},{-1,-1,-1}}};
    	                                                           
    private static final int[][][] aiPoints = new int[][][] { {{2,2,2},{0,1,0},{3,3,3}},{{3,0,2},{3,1,2},{3,0,2}},{{0,2,2},{3,1,2},{3,3,0}},{{2,2,0},{2,1,3},{0,3,3}} };
    private static final int[][][] biPoints = new int[][][] { {{2,2,2},{-1,1,0},{0,1,-1}},{{2,-1,0},{2,1,1},{2,0,-1}},{{-1,1,0},{0,1,-1},{2,2,2}},{{-1,0,2},{1,1,2},{0,-1,2}} };
    
    public static int[][] thinEdges(int[][] edgeImg) {
		int[][] inEdgeImg = edgeImg;
		for(int y = 0; y < inEdgeImg[0].length; ++y)
			for(int x = 0; x < inEdgeImg.length; ++x)
				inEdgeImg[x][y] = (inEdgeImg[x][y] == 0) ? 1 : 0;
		
		int[][] finalPoints = ImageManipulate.getFinalPointsAt(inEdgeImg, 0);
		
		int i = 0;
		int it = 0;
		while(!ImageManipulate.isEqualMinusEdge(inEdgeImg, finalPoints)) {
			if(it > 64) break;
			int[][] ctPoints = ImageManipulate.getContourPointsAt(inEdgeImg, i);
			inEdgeImg = ImageManipulate.twoArrAdd(ImageManipulate.twoArrSub(inEdgeImg, ctPoints), finalPoints);
			i = (i + 1) % 4;
			finalPoints = ImageManipulate.twoArrAdd(finalPoints, ImageManipulate.getFinalPointsAt(inEdgeImg, i));
			++it;
		}
		
		for(int y = 0; y < finalPoints[0].length; ++y)
			for(int x = 0; x < finalPoints.length; ++x)
				finalPoints[x][y] = (finalPoints[x][y] == 1) ? 0 : 255;
		
		System.out.println(it + " iterations");
		
		return finalPoints;
    }
    
    private static boolean isEqualMinusEdge(int[][] in1, int [][] in2) {
    	for(int y = 1; y < in2[0].length - 1; ++y)
    		for(int x = 1; x < in2.length - 1; ++x)
    			if(in1[x][y] != in2[x][y]) return false;
    	
    	return true;
    }
    
    private static int[][] twoArrAdd(int[][] in1, int[][] in2) {
    	int[][] fin = new int[in1.length][in1[0].length];
    	
    	for(int y = 0; y < in1[0].length; ++y)
    		for(int x = 0; x < in1.length; ++x)
    			fin[x][y] = Math.min(1, in1[x][y] + in2[x][y]);
    	
    	return fin;
    }
    
    private static int[][] twoArrSub(int[][] in1, int[][] in2) {
    	int[][] fin = new int[in1.length][in1[0].length];
    	
    	for(int y = 0; y < in1[0].length; ++y)
    		for(int x = 0; x < in1.length; ++x)
    			fin[x][y] = Math.max(0, in1[x][y] - in2[x][y]);
    	
    	return fin;
    }
    
    private static int[][] getContourPointsAt(int[][] edgeImg, int index) {
    	int imgWidth = edgeImg.length;
		int imgHeight = edgeImg[0].length;
    	
    	int[][] ctPoints = new int[imgWidth][imgHeight];
    	for(int j = 0; j < imgHeight; ++j) for(int i = 0; i < imgWidth; ++i) ctPoints[i][j] = 0;
    	
    	for(int y = 1; y < imgHeight - 1; ++y) { // every column
    		for(int x = 1; x < imgWidth - 1; ++x) { // every row
    			boolean isValid = true;
    				
    			for(int j = -1; j < ImageManipulate.contourPoints[index][0].length - 1; ++j) {
    				for(int i = -1; i < ImageManipulate.contourPoints[index].length - 1; ++i) {
    					int val = ImageManipulate.contourPoints[index][j + 1][i + 1];
    						
    					if((val == 0 || val == 1) && isValid) isValid = (val == edgeImg[x + i][y + j]);
    				}
    			}
    				
    			if(isValid) ctPoints[x][y] = 1;
    			else ctPoints[x][y] = 0;
    		}
    	}
    	
    	return ctPoints;
    }
    
    private static int[][] getFinalPointsAt(int[][] edgeImg, int index) {
    	int imgWidth = edgeImg.length;
		int imgHeight = edgeImg[0].length;
    	
    	int[][] finalPoints = new int[imgWidth][imgHeight];
    	for(int j = 0; j < imgHeight; ++j) for(int i = 0; i < imgWidth; ++i) finalPoints[i][j] = 0;
    	
    	int[][] finalbi_first, finalbi_second;
    	switch(index) {
    		case 0:
    			finalbi_first = ImageManipulate.biPoints[0];
    			finalbi_second = ImageManipulate.biPoints[1];
    			break;
    			
    		case 1:
    			finalbi_first = ImageManipulate.biPoints[2];
    			finalbi_second = ImageManipulate.biPoints[3];
    			break;
    			
    		case 2:
    			finalbi_first = ImageManipulate.biPoints[0];
    			finalbi_second = ImageManipulate.biPoints[3];
    			break;
    			
    		case 3:
    			finalbi_first = ImageManipulate.biPoints[1];
    			finalbi_second = ImageManipulate.biPoints[2];
    			break;
    			
    		default: // default to case 0 options, shouldn't happen anyways
    			finalbi_first = ImageManipulate.biPoints[0];
    			finalbi_second = ImageManipulate.biPoints[1];
    			break;
    	}
    	
    	int[][][] fp_kernels = new int[][][] { ImageManipulate.aiPoints[0], ImageManipulate.aiPoints[1], ImageManipulate.aiPoints[2], ImageManipulate.aiPoints[3], finalbi_first, finalbi_second };
    	
    	for(int y = 1; y < imgHeight - 1; ++y) { // every column
    		for(int x = 1; x < imgWidth - 1; ++x) { // every row
    			for(int z = 0; z < fp_kernels.length; ++z) { // every fp
    				int not_edge_in_blue = 0;
    				int not_edge_in_red = 0;
    				boolean isValid = true;
    				
    				for(int j = -1; j < fp_kernels[z][0].length - 1; ++j) {
    					for(int i = -1; i < fp_kernels[z].length - 1; ++i) {
    						int val = fp_kernels[z][j + 1][i + 1];
    						
    						if((val == 0 || val == 1) && isValid) isValid = (val == edgeImg[x + i][y + j]);
    						else if(val == 2) not_edge_in_blue += (edgeImg[x + i][y + j] == 1) ? 0 : 1;
    						else if(val == 3) not_edge_in_red += (edgeImg[x + i][y + j] == 1) ? 0 : 1;
    					}
    				}
    				
    				if(isValid && not_edge_in_blue < 3 && not_edge_in_red < 3) finalPoints[x][y] = 1;
    			}
    		}
    	}
    	
    	return finalPoints;
    }
    
    // clone function is shallow, this copies more
    public static BufferedImage trueCopy(BufferedImage bi) {
		 ColorModel cm = bi.getColorModel();
		 boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		 WritableRaster raster = bi.copyData(null);
		 
		 return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}
}

