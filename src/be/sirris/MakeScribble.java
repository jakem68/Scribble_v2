package be.sirris;

/*
 * Copyright 2013 Ytai Ben-Tsvi. All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ARSHAN POURSOHI OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied.
 */

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;

import static be.sirris.startDialog.*;
//import static com.sun.deploy.uitoolkit.ToolkitStore.dispose;
import static com.sun.java.accessibility.util.AWTEventMonitor.addWindowListener;

/*
 * This class is no longer supported in opencv-3.0.0 for java
 * see : http://stackoverflow.com/questions/25059576/highgui-is-missing-from-opencv-3-0-0-jar
 *
 * import org.opencv.highgui.Highgui;
*/

/**
 * This is a demo of my scribbler algorithm that vectorizes a raster image in an
 * interesting and artistic way. Provide input filenames as command line
 * arguments. Play with the constants below for different results.
 *
 * @author ytai
 */
public class MakeScribble {
    /**
     * This is the ratio between the line width to the overall output image
     * width. It is important, since it tells our algorithm how much darkness a
     * single line contributes.
     */
    private static final double NATIVE_RESOLUTION = 450; //450
    /**
     * Set to true for one continuous stroke, false for discontinuous lines.
     */
    private static final boolean CONTINUOUS = true;
    /**
     * How many candidates to consider for each line segment.
     */
    private static final int NUM_CANDIDATES = 1000;

    //page setup information
    private static final double approachHeight = 15; // mm
    private static double referenceHeight = 6; // mm //set preleminary at 0 for security reasons
    //refToStartX en refToStartY need preferably to be tested beforehand. Robot should start with wrist 3 at +90°
    // (= Waypoint_origin in robot program)
    private static final double refToStartX = 20; // A3=40, distance from reference point to first drawing starting point in X
    private static final double refToStartY = 10; // A3=10, distance from reference point to first drawing starting point in Y
    private static final int numberColumns = 3; // nr of drawing columns that can be drawn in 1 cycle
    private static final int numberRows = 2; // nr of drawing rows that can be drawn in 1 cycle
    private static final double deltaColumn = 337; // distance from column to column (pitch) 40 + 297
    private static final double deltaRow = 460; // distance from row to row (pitch) 40 + 420
    private static final int paperWidth = 297; // A3=297mm
    private static final int paperHeight = 420; // A3=420mm
    private static final int paperWhiteband = 30; // mm is certainly not too much for most pictures.
    /**
     * This tells the algorithm when to stop: when the average darkness in the
     * image is below this threshold.
     */
    public static double THRESHOLD; // 0.2
    public static double THRESHOLD_ORIGINAL = 0.2; // 0.2
    /**
     * Since we're doing all the image calculations in fixed-point, this is a
     * scaling factor we are going to use.
     */
    public static double GRAY_RESOLUTION; // 128;
    public static double GRAY_RESOLUTION_ORIGINAL = 128; // 128;
    /**
     * By how much to down-sample the image.
     */
    public static double SCALE; // 0.2;
    public static double SCALE_ORIGINAL = 0.2; // 0.2;
    public static boolean ROBOT = false;
    private static Random random_ = new Random();
    private static boolean newLine = true;
    //    private static final int robotUnitsRatio = 1000; // output to ROBOT is in 'mm'! if ROBOT expects input in 'm' robotUnitsRatio = 1000
    private static double[] poseTrans = new double[6];
    private static double[] totalDisplacement = new double[poseTrans.length];
    private static double[] poseDrawingOrigin = new double[6];
    private static double outputMultiplicator = 0.0017*1.3; //measured and calculated based on real drawed picture, corrected after first tests

    // initializing the count of drawingposition
    private static double currentColumn = 1;
    private static double currentRow = 1;
    private static double currentStartPointX;
    private static double currentStartPointY;

    private static double maxX = 0;
    private static double maxY = 0;

    static {
         // System.loadLibrary("libopencv_java320.so");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void run(String[] args) throws IOException {
        String filename = args[0];

        Mat original = Imgcodecs.imread(filename);

        //Rotate original to make long side stand up
        if (original.width()>original.height()) {
            Core.transpose(original, original);
            Core.flip(original, original, 1);
        }

        // Convert to gray-scale.
        Imgproc.cvtColor(original, original, Imgproc.COLOR_BGR2GRAY);

        // Resize to native resolution.
        Mat preview = new Mat();
        final double scale = NATIVE_RESOLUTION / original.cols();
        Imgproc.resize(original, preview, new Size(), scale, scale, Imgproc.INTER_AREA);

        // Down-sample.
        Mat in = new Mat();
        Imgproc.resize(preview, in, new Size(), SCALE, SCALE, Imgproc.INTER_AREA);

        // Negative: bigger number = darker.
        final Mat scalar = new Mat(1, 1, CvType.CV_64FC1).setTo(new Scalar(255));
        Core.subtract(scalar, in, in);

        // Convert to S16: we need more color resolution and negative numbers.
        in.convertTo(in, CvType.CV_16SC1);

        // We scale such that for each line we can subtract GRAY_RESOLUTION and it will correspond to darkening by SCALE.
        Core.multiply(in, new Scalar(GRAY_RESOLUTION / SCALE / 255), in);

//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//        }

        final JDialog frame = new JDialog();
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {

                try {
                    frame.dispose();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        Container contentPane = frame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.X_AXIS));

        contentPane.add(new JLabel(new ImageIcon(matToBufferedImage(preview))));
        ImageComponent component = new ImageComponent(matToBufferedImage(preview));
        contentPane.add(component);

        frame.pack();
        frame.setVisible(true);

        // Now is the actual algorithm!
        Point lastP = null;
        double residualDarkness = average(in) / GRAY_RESOLUTION * SCALE;

        //if settingsfileExists(false) calculate threshold as percentage of residualDarkness
        if (_settingsFileExists==false){
            THRESHOLD = (residualDarkness*0.55);

        }

        double totalLength = 0;
        int lines = 0;
        component.hideImage();

        // start new server only if ROBOT has been selected AND move to startpoint of current drawing.
        if (ROBOT) {
            newLine = true;

            //calculate row en column
            currentRow = Math.floor(_drawingCounter /3)+1;
            currentColumn =  (_drawingCounter -((currentRow-1)*numberColumns))+1;

            if (currentRow > numberRows) {
                System.out.println("You are drawing more drawings then positions available!");
                System.out.println("The program will stop!");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                System.exit(0);
            }

            //calculate compensated referenceHeight to compensate for location on the table
            referenceHeight = 6;
            int compensation;
            switch (_drawingCounter) {
                case 0:  compensation= 0;
                    break;
                case 1:  compensation= 3;
                    break;
                case 2:  compensation= 4;
                    break;
                case 3:  compensation= 0;
                    break;
                case 4:  compensation= 1;
                    break;
                case 5:  compensation= 2;
                    break;
                default: compensation= 0;
                    break;
            }

            referenceHeight += compensation;

            _drawingCounter += 1;
            currentStartPointX = refToStartX + paperWhiteband + ((currentColumn - 1) * deltaColumn);
            currentStartPointY = refToStartY + paperWhiteband + ((currentRow - 1) * deltaRow);

            // move to startXY (50mm up from origin is hard coded in UR program)
            poseTrans[1] = -currentStartPointX; // x value
            poseTrans[0] = -currentStartPointY; // y value
            poseTrans[2] = 0; // z value
            poseTrans[3] = 0; // a value
            poseTrans[4] = 0; // b value
            poseTrans[5] = 0; // c value
            poseDrawingOrigin = poseTrans;
            sendMessage(poseTrans, out);
        }

        // create individual parts of lines
        while (residualDarkness > THRESHOLD) {
            final Point[] bestLine = nextLine(in, NUM_CANDIDATES, CONTINUOUS ? lastP : null);
            final Point[] scaledLine = scaleLine(bestLine, 1 / SCALE);
            lastP = bestLine[1];
            List<int[]> line = new ArrayList<int[]>(2);
            line.add(new int[]{(int) scaledLine[0].x, (int) scaledLine[0].y});
            line.add(new int[]{(int) scaledLine[1].x, (int) scaledLine[1].y});
            totalLength += Math.hypot(scaledLine[0].x - scaledLine[1].x, scaledLine[0].y - scaledLine[1].y);
            component.addLine(line);
            ++lines;
            residualDarkness = average(in) / GRAY_RESOLUTION * SCALE;
            System.out.format("%d -- remaining darkness: %.0f%% length: %.1f\n", lines, 100 * residualDarkness,
                    totalLength);


            // check for maximum point in x and y direction (print to system.log at the end of scribble)
            if ((scaledLine[1].x * (calculatePaperScale(in))) > maxX) {
            maxX = scaledLine[1].x * (calculatePaperScale(in));
            }

            if ((scaledLine[1].y * (calculatePaperScale(in))) > maxY) {
            maxY = scaledLine[1].y * (calculatePaperScale(in));
            }



            if (ROBOT) {
                //
                //when starting a new line make first movement in air
                poseTrans[1] = ((scaledLine[1].x) * -(calculatePaperScale(in))) - currentStartPointX;// * finalScale; // x value
                poseTrans[0] = ((scaledLine[1].y) * -(calculatePaperScale(in))) - currentStartPointY;// * finalScale; // y value
//                    poseTrans[2] = 0; // z value
//                    poseTrans[3] = 0; // a value
//                    poseTrans[4] = 0; // b value
//                    poseTrans[5] = 0; // c value
                sendMessage(poseTrans, out);
                //then move to paper, down and mark newLine as false
                if (newLine) {
                    // move to paper
//                poseTrans[0] = 0; // x value
//                poseTrans[1] = 0; // y value
                    poseTrans[2] = referenceHeight + 50; // z value, 50 is hardcoded in UR program
//                poseTrans[3] = 0; // a value
//                poseTrans[4] = 0; // b value
//                poseTrans[5] = 0; // c value
                    sendMessage(poseTrans, out);
                    newLine = false;
                }
            }
        }

        //after finishing the scribble send maxX and maxY of the scribble to system.log
        System.out.print("TAG: (maxX, maxY) = (");
        System.out.print(maxX);
        System.out.print(", ");
        System.out.print(maxY);
        System.out.println(")");

        //check whether either maxX or maxX exceeds allowable value, allowable value = paper size - whiteband
        if (maxX > (paperWidth-2*paperWhiteband)){
            System.out.print("ATTENTION: width of the drawing ");
            System.out.print(maxX);
            System.out.print(" exceeds effective width of the paper ");
            System.out.print(paperWidth-2*paperWhiteband);
            System.out.println("!!!");

        }
        if (maxY > (paperHeight-2*paperWhiteband)){
            System.out.print("ATTENTION: height of the drawing ");
            System.out.print(maxY);
            System.out.print(" exceeds effective height of the paper ");
            System.out.print(paperHeight-2*paperWhiteband);
            System.out.println("!!!");

        }




        // after finishing the scribble move ROBOT to referenceheight +50
        if (ROBOT) {
            // move away from paper
//            poseTrans[0] = 0; // x value
//            poseTrans[1] = 0; // y value
            poseTrans[2] = -referenceHeight - 50;
//            poseTrans[3] = 0; // a value
//            poseTrans[4] = 0; // b value
//            poseTrans[5] = 0; // c value
            sendMessage(poseTrans, out);
            // move to 50 above origin, mark newLine as true for next scribble
            poseTrans[0] = 0; // x value
            poseTrans[1] = 0; // y value
//            poseTrans[2] = -referenceHeight - 50; // z value, 50 is hardcoded in UR program
//            poseTrans[3] = 0; // a value
//            poseTrans[4] = 0; // b value
//            poseTrans[5] = 0; // c value
            sendMessage(poseTrans, out);
            newLine = true;
        }
    }

    // Line dimensions are NOT based on pixels Mat in
    public static double calculatePaperScale(Mat in) {
        // get the rows and columns of the picture used to generate lines
        double pixelsWidth = in.cols();
        double pixelsHeight = in.rows();
        double paperScale;
        double effectivePW = paperWidth - (2 * paperWhiteband); // effectivePaperWidth
        double effectivePH = paperHeight - (2 * paperWhiteband); // effectivePaperHeight

        // depending on relation width vs height decide whether to resize currentxy output based on width or height for papersize and whiteband specified above
        if ((pixelsWidth / pixelsHeight) > (paperWidth / paperHeight)) {
            paperScale = (effectivePW * outputMultiplicator);
        } else {
            paperScale = (effectivePH * outputMultiplicator);
        }
        //the outputmultiplicator was calculated based on a setting of 450, therefore compensation is required in case NATIVE_RESOLUTION was changed to something else then 450, since NATIVE_RESOLUTION influences the output size
        paperScale = paperScale * (450 / NATIVE_RESOLUTION);
        System.out.println(paperScale);
        return paperScale;
    }

    private static void sendMessage(double[] poseTrans, PrintWriter out) {
        String[] messageArr = new String[6];

        //create array of strings
        for (int i = 0; i < poseTrans.length; i++) {
            messageArr[i] = Double.toString(poseTrans[i]);
        }
        //create final String to send
        String message = "(" + messageArr[0] + "," + messageArr[1] + "," + messageArr[2] + "," + messageArr[3] + ","
                + messageArr[4] + "," + messageArr[5] + ")";
        out.println(message);
        System.out.println(message);
        System.out.print(" ");
    }

    private static double average(Mat in) {
        double total = Core.sumElems(in).val[0];
        return total / in.cols() / in.rows();
    }

    /**
     * Gets the best of several random lines.
     * <p>
     * The number of candidates is determined by the numAttempts argument. The
     * criterion for determining the winner is the one which covers the highest
     * average darkness in the image. As a side-effect, the winner will be
     * subtracted from the image.
     *
     * @param image       The image to approximate. Expected to be of floating point
     *                    format, with higher values representing darker areas. Should
     *                    be scaled such that subtracting a value of GRAY_RESOLUTION
     *                    from a pixel corresponds to how much darkness a line going
     *                    through it adds. When the method returns, the winning line
     *                    will be subtracted from this image.
     * @param numAttempts How many candidates to examine.
     * @param startPoint  Possibly, force the line to start at a certain point. In case
     *                    of null, the line will comprise two random point.
     * @return The best line.
     */
    private static Point[] nextLine(Mat image, int numAttempts, Point startPoint) {
        Mat mask = Mat.zeros(image.size(), CvType.CV_8U);
        Mat bestMask = Mat.zeros(image.size(), CvType.CV_8U);
        Point[] line = new Point[2];
        Point[] bestLine = null;
        double bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < numAttempts; ++i) {
            generateRandomLine(image.size(), startPoint, line);

            // Calculate the score for this line as the average darkness over
            // it.
            // This way to calculate this is crazy inefficient, but compact...
            mask.setTo(new Scalar(0));
            Imgproc.line(mask, line[0], line[1], new Scalar(GRAY_RESOLUTION));
            double score = Core.mean(image, mask).val[0];

            if (score > bestScore) {
                bestScore = score;
                Mat t = mask;
                mask = bestMask;
                bestMask = t;
                bestLine = line.clone();
            }
        }
        Core.subtract(image, bestMask, image, bestMask, image.type());
        return bestLine;
    }

    private static Point[] scaleLine(Point[] line, double scale) {
        Point[] scaledLine = new Point[2];
        scaledLine[0] = new Point(line[0].x * scale, line[0].y * scale);
        scaledLine[1] = new Point(line[1].x * scale, line[1].y * scale);
        return scaledLine;
    }

    private static void generateRandomLine(Size s, Point pStart, Point[] result) {
        if (pStart == null) {
            result[0] = new Point(random_.nextDouble() * s.width, random_.nextDouble() * s.height);
        } else {
            result[0] = pStart;
        }
        do {
            result[1] = new Point(random_.nextDouble() * s.width, random_.nextDouble() * s.height);
        } while (result[0].equals(result[1]));
    }

    public static BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();
        int rows = matrix.rows();
        int elemSize = (int) matrix.elemSize();
        byte[] data = new byte[cols * rows * elemSize];
        int type;

        matrix.get(0, 0, data);

        switch (matrix.channels()) {
            case 1:
                type = BufferedImage.TYPE_BYTE_GRAY;
                break;

            case 3:
                type = BufferedImage.TYPE_3BYTE_BGR;

                // bgr to rgb
                byte b;
                for (int i = 0; i < data.length; i = i + 3) {
                    b = data[i];
                    data[i] = data[i + 2];
                    data[i + 2] = b;
                }
                break;

            default:
                return null;
        }

        BufferedImage image = new BufferedImage(cols, rows, type);
        image.getRaster().setDataElements(0, 0, cols, rows, data);

        return image;
    }

    private static class ImageComponent extends Component {
        private static final long serialVersionUID = -8921722655371221897L;

        private final BufferedImage image_;
        private final List<List<int[]>> lines_ = new LinkedList<List<int[]>>();
        private boolean showImage_ = true;

        public ImageComponent(BufferedImage image) {
            image_ = image;
        }

        public synchronized void hideImage() {
            showImage_ = false;
            repaint();
        }

        public synchronized void addLine(List<int[]> line) {
            lines_.add(line);
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(image_.getWidth(), image_.getHeight());
        }

        @Override
        public synchronized void paint(Graphics g) {
            if (showImage_) {
                g.drawImage(image_, 0, 0, image_.getWidth(), image_.getHeight(), 0, 0, image_.getWidth(),
                        image_.getHeight(), null);
            } else {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, image_.getWidth(), image_.getHeight());
            }
            g.setColor(Color.BLACK);
            for (List<int[]> line : lines_) {
                final Iterator<int[]> iter = line.iterator();
                int[] prev = iter.next();
                while (iter.hasNext()) {
                    int next[] = iter.next();
                    g.drawLine(prev[0], prev[1], next[0], next[1]);
                    prev = next;
                }
            }
        }
    }
}
