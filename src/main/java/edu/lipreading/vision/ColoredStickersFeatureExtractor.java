package edu.lipreading.vision;

//import java.awt.Canvas;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;
import static com.googlecode.javacv.cpp.opencv_core.cvInRangeS;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_highgui.cvSaveImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_MEDIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetCentralMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvGetSpatialMoment;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMoments;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import java.util.Vector;

import javax.swing.JPanel;

import com.googlecode.javacv.CanvasFrame;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core.CvScalar;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc.CvMoments;

import edu.lipreading.Sample;
import edu.lipreading.Utils;

public class ColoredStickersFeatureExtractor extends AbstractFeatureExtractor{
	private final static short NUM_OF_STICKERS = 4;

	private final static short RED_VECTOR_INDEX = 0;
	private final static short GREEN_VECTOR_INDEX = 1;
	private final static short BLUE_VECTOR_INDEX = 2;
	private final static short YELLOW_VECTOR_INDEX = 3;


	private final static CvScalar RED_MIN = cvScalar(20, 40, 200, 0); 
	private final static CvScalar RED_MAX = cvScalar(40, 80, 255, 0);

	private final static CvScalar GREEN_MIN = cvScalar(0, 30, 0, 0);
	private final static CvScalar GREEN_MAX = cvScalar(70, 255, 70, 0);

	private final static CvScalar BLUE_MIN = cvScalar(70, 0, 10, 0);
	private final static CvScalar BLUE_MAX = cvScalar(170, 60, 70, 0);

	private final static CvScalar YELLOW_MIN = cvScalar(20, 100, 100, 0);
	private final static CvScalar YELLOW_MAX = cvScalar(50, 255, 255, 0);

	@Override
	protected Sample getPoints() throws Exception, InterruptedException {
		Sample sample = new Sample(sampleName);
		IplImage grabbed;
		CanvasFrame frame = null;
		CanvasFrame painter = new CanvasFrame("Stickers Detection - " + sampleName);
		JPanel pointsPanel = null;
		if(!Utils.isCI()){
			frame = new CanvasFrame("output", CanvasFrame.getDefaultGamma()/grabber.getGamma());
			frame.setDefaultCloseOperation(CanvasFrame.EXIT_ON_CLOSE);
			pointsPanel = new JPanel();
			painter.setContentPane(pointsPanel);
		}

		while((grabbed = grabber.grab()) != null){
			List<Integer> frameCoordinates = new Vector<Integer>();
			frameCoordinates.addAll(getCoordinatesOfObject(grabbed, RED_MIN, RED_MAX));
			frameCoordinates.addAll(getCoordinatesOfObject(grabbed, GREEN_MIN, GREEN_MAX));
			frameCoordinates.addAll(getCoordinatesOfObject(grabbed, BLUE_MIN, BLUE_MAX));
			frameCoordinates.addAll(getCoordinatesOfObject(grabbed, YELLOW_MIN, YELLOW_MAX));
			
			sample.getMatrix().add(frameCoordinates);
			if(!Utils.isCI()){
				frame.showImage(grabbed);
				painter.setSize(frame.getCanvasSize());
			}
			for(int i = 0; i < NUM_OF_STICKERS; i++){

				if(!Utils.isCI()){
					Graphics graphics = pointsPanel.getGraphics();
					switch (i){
					case RED_VECTOR_INDEX:
						graphics.setColor(Color.RED);
						break;
					case GREEN_VECTOR_INDEX:
						graphics.setColor(Color.GREEN);
						break;
					case BLUE_VECTOR_INDEX:
						graphics.setColor(Color.BLUE);
						break;
					case YELLOW_VECTOR_INDEX:
						graphics.setColor(Color.YELLOW);
						break;
					}
					graphics.drawOval(frameCoordinates.get(i * 2), frameCoordinates.get((i * 2) + 1), 10, 10);
				}
			}
		}
		if(!Utils.isCI()){
			frame.dispose();
		}
		return sample;
	}


	private IplImage getThresholdImage(IplImage orgImg, CvScalar minScalar, CvScalar maxScalar) {
		IplImage imgThreshold = cvCreateImage(cvGetSize(orgImg), 8, 1);
		cvInRangeS(orgImg, minScalar, maxScalar, imgThreshold);
		cvSmooth(imgThreshold, imgThreshold, CV_MEDIAN, 15);
		cvSaveImage("dsmthreshold.jpg", imgThreshold);
		return imgThreshold;
	}
	
	private List<Integer> getCoordinatesOfObject(IplImage img, CvScalar scalarMin,
			CvScalar scalarMax) {
		IplImage detectThrs = getThresholdImage(img, scalarMin, scalarMax);
		CvMoments moments = new CvMoments();
		cvMoments(detectThrs, moments, 1);
		double mom10 = cvGetSpatialMoment(moments, 1, 0);
		double mom01 = cvGetSpatialMoment(moments, 0, 1);
		double area = cvGetCentralMoment(moments, 0, 0);
		int posX = (int) (mom10 / area);
		int posY = (int) (mom01 / area);
		List<Integer> ans = new Vector<Integer>();
		ans.add(posX);
		ans.add(posY);
		return ans;
	}

}
