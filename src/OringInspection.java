
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class OringInspection {
	
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Create and set up the window.
		JFrame frame = new JFrame("OpenCV");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JLabel imageHolder = new JLabel();
		frame.getContentPane().add(imageHolder, BorderLayout.CENTER);

		// Display the window.
		frame.pack();
		frame.setVisible(true);

		// press Q to quit application
		frame.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_Q)
					System.exit(0);
			}

			public void keyReleased(KeyEvent arg0) {
			}

			public void keyTyped(KeyEvent arg0) {
			}
		});

		System.out.println("Stream Opened");

		Mat img = new Mat();
		Mat out = new Mat();
		BufferedImage jimg;
		Mat histim;
		
		int labels[] = null;
		int ringLabel;
		int edges[] = null;
		boolean passImage;
		
		int i = 1;
		while (true) {
			// reading image
			img = Imgcodecs
					.imread("C:\\Users\\Igor\\Dysk Google\\ITB\\Semestr 8\\Lab\\Computer Vision\\workspace\\Oring\\images\\Oring"
							+ (i) + ".jpg");

			System.out.println("Image: " + i);
			// gets image histogram
			int[] h = hist(img);
			
			//calculate the mean processing time per frame and display it
	        double before = (double)System.nanoTime()/1000000000;

			// convert to greyscale
			Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
			histim = new Mat(256, 256, CvType.CV_8UC3);
			drawHist(histim, h);

			// threshold the image
			threshold(img, findHistPeak(h) - 50);

			// fix gaps
			dilate(img);
			erode(img);
			erode(img);
			erode(img);

			// object segmentation
			labels = labelPixels(img);
			ringLabel = countLabels(img, labels);

			// display only image of the ring
			removeNoise(img, ringLabel, labels);

			// find ring edges
			edges = getEdges(img, labels, ringLabel);
			
			// process the image
			passImage = processImage(img, edges, labels);
			
			Imgproc.cvtColor(img, out, Imgproc.COLOR_GRAY2BGR);
			
			double after = (double)System.nanoTime()/1000000000;
			
			if(passImage)
				Imgproc.putText(out, "Pass", new Point(10,30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,255,0));
			else {
				Imgproc.putText(out, "Fail", new Point(10,30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0,0,255));
			}
			
			System.out.println("Pass Image: " + passImage);
			System.out.println("Processing time: " + (after-before) + " seconds.");
			
			// convert to a Java BufferedImage so we can display in a label
			jimg = Mat2BufferedImage(out);
			imageHolder.setIcon(new ImageIcon(jimg));
			frame.pack();
			i++;

			if (i == 16)
				i = 1;

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("");
		}
	}

	
	public static int sumArray(int[] array) {
		int sum = 0;
		for(int i = 0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}
	
	public static boolean checkRing(Mat img, int Xcenter, int highestY, int lowestY, int[] labels) {
		int[] rightPoints;
		int[] leftPoints;
		
		int sumRight = 0;
		int sumLeft = 0;
		int tolerance = 7;
		
		int currentY = lowestY;
		
		boolean passImage = true;
		
		while(currentY <= highestY) {
			rightPoints = new int[img.cols() - Xcenter];
			leftPoints = new int[Xcenter];
			
			// fill left points
			for(int i = 0; i < leftPoints.length; i++) {
				int point = (currentY * img.rows()) + i; 
				
				if(labels[point] == 1) {
					leftPoints[i] = 1;
				} else {
					leftPoints[i] = 0;
				}
			}
			
			// fill right points
			for(int j = rightPoints.length - 1; j >= 0; j--) {
				int point = ((currentY * img.rows()) + (img.cols() - j));
				
				if(labels[point] == 1) {
					rightPoints[j] = 1;
				} else {
					rightPoints[j] = 0;
				}
			}
			
			// sum the points
			sumRight = sumArray(rightPoints);
			sumLeft = sumArray(leftPoints);
			
			currentY++;
		
			if(Math.abs(sumRight - sumLeft) < tolerance) {
				sumRight = 0;
				sumLeft = 0;
			} else {
				passImage = false;
				break;
			}
		}
		
		return passImage;
	}
	
	public static boolean processImage(Mat img, int[] edges, int[] labels) {	
		int top = 0;
		int bottom = 0;
		int left = 0;
		int right = 0;
		
		int highestX = 0;
		int lowestX = img.rows();
		int highestY = 0;
		int lowestY = img.cols();
		
		for(int i = 0; i < edges.length; i++) {
			int x = 0;
			int y = 0;
			
			if(i > 0) {
				y = (int)(Math.floor((i / img.rows())));
				x = ((y * img.rows()) - i) * (-1);
			}
			
			if(edges[i] == 1) {
				if(edges[i - img.cols()] == 0 && y < lowestY) {
					top = i;
					lowestY = y;
				}
				
				if(edges[i + 1] == 0 && edges[i + img.cols()] == 1 && x > highestX) {
					right = i;
					highestX = x;
				}
				
				if(edges[i + img.cols()] == 0 && y > highestY) {
					bottom = i;
					highestY = y;
				}
				
				if(edges[i - 1] == 0 && edges[i - img.cols()] == 1 && x < lowestX) {
					left = i;
					lowestX = x;
				}
			}
		}
		
		int Xcenter = ((highestX - lowestX) / 2) + lowestX;
		int Ycenter = ((highestY - lowestY) / 2) + lowestY;
		int center = ((Ycenter * img.rows()) + Xcenter);
		
		System.out.println("Properties: ");
		System.out.println("\t\tTOP: " + top + " RIGHT: " + right + " BOTTOM: " + bottom + " LEFT: " + left);
		System.out.println("\t\thighestX: " + highestX + " lowestX: " + lowestX);
		System.out.println("\t\thighestY: " + highestY + " lowestY: " + lowestY);
		System.out.println("\t\tXcenter: " + Xcenter);
		System.out.println("\t\tYcenter: " + Ycenter);
		System.out.println("\t\tCenter: " + center);
		
		boolean passImage = checkRing(img, Xcenter, highestY, lowestY, labels);
		
		return passImage;
	}
	
	public static void printArray(int[] arr) {
		int k = 0;
		for(int i = 0; i < arr.length; i++) {
			System.out.print(arr[i]);
			k++;
			
			if(k == 220) {
				k = 0;
				System.out.println("");
			}
		}
	}
	
	public static int[] getEdges(Mat img, int[] labels, int ringLabel) {
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		int[] edges = new int[labels.length];
		
		for(int i = 0; i < labels.length; i++) {
			if(labels[i] == ringLabel) {
				// if (top or bottom) and (left or right) neighbor is background
				if(((data[i - img.cols()] & 0xFF) == 0 || (data[i + img.cols()] & 0xFF) == 0) || ((data[i - 1] & 0xFF) == 0 || (data[i + 1] & 0xFF) == 0)) {
					edges[i] = 1;
				} else {
					edges[i] = 0;
				}
			}
		}
		
		return edges;
	}
	
	public static void removeNoise(Mat img, int ringLabel, int[] labels) {
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		for(int i = 0; i < data.length; i++) {
			if(labels[i] != ringLabel)
				data[i] = (byte)0;
		}
		
		img.put(0, 0, data);
	}
	
	public static int countLabels(Mat img, int[] labels) {
		Map<Integer, Integer> labelsCount = new HashMap<Integer, Integer>(); 
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		int currLabel = 0;
		int ringSamples = 0;
		int ringLabel = 0;
		
		for(int i = 0; i < labels.length; i++) {
			if((data[i] & 0xFF) == 255 && labels[i] == currLabel) {	// Is foreground, is current label
				if(labelsCount.get(currLabel) == null)
					labelsCount.put(currLabel, 1);
				else
					labelsCount.put(currLabel, (labelsCount.get(currLabel)+1));
			} else if((data[i] & 0xFF) == 255 && labels[i] > currLabel) { // Is foreground, is another label
				currLabel++;
				labelsCount.put(currLabel, 1);
			}
		}
		
		for(int j = 1; j <= labelsCount.size(); j++) {
			if(j == 1) {
				ringSamples = labelsCount.get(j);
				ringLabel = j;
			}
			else if(j > 1 && labelsCount.get(j) > ringSamples) { // Is more than 1 label, is bigger than previous
				ringSamples = labelsCount.get(j);
				ringLabel = j;
			}
		}
		
		if(labelsCount.size() > 1)
			System.out.println("Ring labeled as: " + ringLabel + ". Removing other objects.");
		
		return ringLabel;
	}
	
	public static int[] labelPixels(Mat img) {
		MyQueue q = new MyQueue();
		int currLabel = 0;
		int[] labels = new int[img.rows() * img.cols() * img.channels()];
		
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		
		for(int i = 0; i < data.length; i++) {
			if((data[i] & 0xFF) == 255 && labels[i] == 0) { // got a white pixel that is not labeled
				currLabel++;
				labels[i] = currLabel;
				q.enqueue(i);
				
				while(!q.isEmpty()) {
					int pos = q.dequeue();
					
					if((data[pos+1] & 0xFF) == 255 && labels[pos+1] == 0) { // check right neighbor
						labels[pos+1] = currLabel;
						q.enqueue(pos+1);
					} 
					
					if ((data[pos-1] & 0xFF) == 255 && labels[pos-1] == 0) { // check left neighbor
						labels[pos-1] = currLabel;
						q.enqueue(pos-1);
					} 
					
					if((data[pos-img.cols()] & 0xFF) == 255 && labels[pos-img.cols()] == 0) { // check top neighbor
						labels[pos-img.cols()] = currLabel;
						q.enqueue(pos-img.cols());
					} 
					
					if((data[pos+img.cols()] & 0xFF) == 255 && labels[pos+img.cols()] == 0) { // check bottom neighbor
						labels[pos+img.cols()] = currLabel;
						q.enqueue(pos+img.cols());
					}
				}
			}
		}
		System.out.println("Objects found: " + currLabel);
		
		return labels;
	}

	public static void erode(Mat img) {
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		byte copy[] = data.clone();

		for (int i = 0; i < data.length; i++) {

			int[] neighbors = { 
					i + 1, // right
					i - 1, // left
					i - img.cols(), // top
					i + img.cols(), // bottom
					i + img.cols() + 1, // bottom right
					i + img.cols() - 1, // bottom left
					i - img.cols() + 1, // top right
					i - img.cols() - 1 // top left
			};

			try {
				for (int j = 0; j < neighbors.length; j++) {
					if ((copy[neighbors[j]] & 0xFF) == 0)
						data[i] = (byte) 0;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {

			}
		}

		img.put(0, 0, data);
	}
	
	public static void dilate(Mat img) {
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		byte copy[] = data.clone();

		for (int i = 0; i < data.length; i++) {

			int[] neighbors = { 
					i + 1, // right
					i - 1, // left
					i - img.cols(), // top
					i + img.cols(), // bottom
					i + img.cols() + 1, // bottom right
					i + img.cols() - 1, // bottom left
					i - img.cols() + 1, // top right
					i - img.cols() - 1 // top left
			};

			try {
				for (int j = 0; j < neighbors.length; j++) {
					if ((copy[neighbors[j]] & 0xFF) == 255)
						data[i] = (byte) 255;
				}
			} catch (ArrayIndexOutOfBoundsException ex) {

			}
		}

		img.put(0, 0, data);
	}

	public static int findHistPeak(int[] hist) {
		int largest_count = hist[0];
		int largest_GL = 0;

		for (int i = 0; i < hist.length; i++) {
			if (hist[i] > largest_count) {
				largest_count = hist[i];
				largest_GL = i;
			}
		}

		return largest_GL;
	}

	public static void threshold(Mat img, int threshold) {
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);

		System.out.println("Threshold = " + threshold);

		for (int i = 0; i < data.length; i++) {
			int unsigned = (data[i] & 0xff);
			if (unsigned > threshold)
				data[i] = (byte) 0;
			else
				data[i] = (byte) 255;
		}
		img.put(0, 0, data);
	}

	public static int[] hist(Mat img) {
		int hist[] = new int[256];
		byte data[] = new byte[img.rows() * img.cols() * img.channels()];
		img.get(0, 0, data);
		for (int i = 0; i < data.length; i++) {
			hist[(data[i] & 0xff)]++;
		}
		return hist;
	}

	public static void drawHist(Mat img, int[] hist) {
		// get max hist value for range adjustment
		int max = 0;
		for (int i = 0; i < hist.length; i++) {
			if (hist[i] > max)
				max = hist[i];
		}
		int scale = max / 256;
		for (int i = 0; i < hist.length - 1; i++) {
			// Core.circle(img, new Point(i*2+1,img.rows()-(hist[i]/scale)+1),
			// 1, new Scalar(0,255,0));
			Imgproc.line(img, new Point(i + 1, img.rows() - (hist[i] / scale) + 1),
					new Point(i + 2, img.rows() - (hist[i + 1] / scale) + 1), new Scalar(0, 0, 255));
		}
	}

	public static BufferedImage Mat2BufferedImage(Mat m) {
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if (m.channels() > 1) {
			type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels() * m.cols() * m.rows();
		byte[] b = new byte[bufferSize];
		m.get(0, 0, b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(), m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);
		return image;

	}
}