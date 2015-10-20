import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import ij.*;
import ij.gui.*;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.plugin.*;
import ij.plugin.filter.Analyzer;
import ij.process.*;


public class MTF_Analyzer implements PlugIn{


	@Override
	public void run(String arg) {	
		TestFrame mainFrame = new TestFrame("MTF Analyzer");
		mainFrame.display();
		
	}
	
}
class CustomUtils{
	private static final Roi roi = new Roi(0,0,0,0);
	public static  Plot drawPlot(TreeMap<Double,Double> map,String headline, String xName, String yName){ 
		Set<Double> keys = map.keySet();
	
		double[] plotX = new double[keys.size()];
		double[] plotY = new double[keys.size()];
		int count = 0;
		for(Double key : keys){
		
			plotX[count]=key;
			plotY[count]=map.get(key);		
			count++;
		}
		double minX = Collections.min(keys);
		double maxX = Collections.max(keys);
		double minY = Collections.min(map.values());
		double maxY = Collections.max(map.values());
		double insetX = (maxX == 0) ? Math.abs(minX*0.05) : Math.abs(maxX*0.05);
		double insetY = (maxY == 0) ? Math.abs(minY*0.05) : Math.abs(maxY*0.05);
		
		Plot resultantGraph = new Plot(headline, 
										xName, 
										yName, 
										plotX,
										plotY);
		//resultantGraph.setSize(550, 300);
		resultantGraph.setColor(Color.BLACK);
		resultantGraph.setLimits( minX-insetX, maxX+insetX,  minY-insetY, maxY+insetY); 
		Window topWindow = WindowManager.getFrontWindow();
		PlotWindow pw = resultantGraph.show();
		
		if(topWindow != null){
			System.out.println("X : " + pw.getX() + " Y : " + pw.getY());
			pw.setLocation(topWindow.getX() + 50, topWindow.getY()+50);
			System.out.println("X : " + pw.getX() + " Y : " + pw.getY());
		}
		pw.requestFocus();
		pw.toFront();
		
		return resultantGraph;
	}
	public static Plot drawContrastPlot(TreeMap<Double,ArrayList<Double>> map, int imageHeight, String headline, String xName, String yName){
		
		Set<Double> keys = map.keySet();
		double[] plotX = new double[keys.size()];
		double[] plotY = new double[keys.size()];
		int count = 0;
		for(Double key : keys){
			//System.out.println("KEY:"+key+"   "+"VALUE:"+distanceValueAverageMap.get(key));
			//if(count>200)break;
			plotX[count]=key;
			plotY[count]=map.get(key).get(0);	  // 1 - to get sum	
			count++;
		}
		
		Plot resultantGraph = new Plot(headline, 
										xName, 
										yName, 
										plotX,
										plotY);
		resultantGraph.setSize(550, 300);
		resultantGraph.setColor(Color.BLACK);
		resultantGraph.setLimits(0.0, imageHeight/2 + 200, 0.0, 1.3);   ///Konstrast
		Window topWindow = WindowManager.getFrontWindow();		
		PlotWindow pw = resultantGraph.show();
		
		if(topWindow != null){
			System.out.println("X : " + pw.getX() + " Y : " + pw.getY());
			pw.setLocation(topWindow.getX() + 50, topWindow.getY()+50);
			System.out.println("X : " + pw.getX() + " Y : " + pw.getY());
		}
		pw.requestFocus();
		pw.toFront();
		
		 return resultantGraph;
	}
	static void addCurveToPlot(Plot plot, Color color, TreeMap<Double,Double> map){
		Set<Double> keys = map.keySet();
		double[] plotX = new double[keys.size()];
		double[] plotY = new double[keys.size()];
		int count = 0;
		for(Double key : keys){
			plotX[count]=key;
			plotY[count]=map.get(key);		
			count++;
		}
		plot.setColor(color);
		plot.addPoints(plotX, plotY, Plot.LINE);
		
	}
	static void addCurveToContrastPlot(Plot plot, Color color, TreeMap<Double, ArrayList<Double>> map){
		Set<Double> keys = map.keySet();
		double[] plotX = new double[keys.size()];
		double[] plotY = new double[keys.size()];
		int count = 0;
		for(Double key : keys){
			plotX[count]=key;
			plotY[count]=map.get(key).get(0);		
			count++;
		}
		plot.setColor(color);
		plot.addPoints(plotX, plotY, Plot.LINE);
		
	}
	public static void resetRoiColor(){
		roi.setColor(Color.YELLOW);
	}
	public static void setDescriptionStyle(Component c){
		Font f = new Font("SansSerif", Font.PLAIN, 11);
		c.setFont(f);
		c.setForeground(Color.DARK_GRAY);
	}
	public static boolean stringParsableToDouble(String s){
		final String Digits     = "(\\p{Digit}+)";
		final String HexDigits  = "(\\p{XDigit}+)";
		// an exponent is 'e' or 'E' followed by an optionally 
		// signed decimal integer.
		final String Exp        = "[eE][+-]?"+Digits;
		final String fpRegex    =
		            ("[\\x00-\\x20]*"+  // Optional leading "whitespace"
		             "[+-]?(" + // Optional sign character
		             "NaN|" +           // "NaN" string
		             "Infinity|" +      // "Infinity" string

		             // A decimal floating-point string representing a finite positive
		             // number without a leading sign has at most five basic pieces:
		             // Digits . Digits ExponentPart FloatTypeSuffix
		             // 
		             // Since this method allows integer-only strings as input
		             // in addition to strings of floating-point literals, the
		             // two sub-patterns below are simplifications of the grammar
		             // productions from the Java Language Specification, 2nd 
		             // edition, section 3.10.2.

		             // Digits ._opt Digits_opt ExponentPart_opt FloatTypeSuffix_opt
		             "((("+Digits+"(\\.)?("+Digits+"?)("+Exp+")?)|"+

		             // . Digits ExponentPart_opt FloatTypeSuffix_opt
		             "(\\.("+Digits+")("+Exp+")?)|"+

		       // Hexadecimal strings
		       "((" +
		        // 0[xX] HexDigits ._opt BinaryExponent FloatTypeSuffix_opt
		        "(0[xX]" + HexDigits + "(\\.)?)|" +

		        // 0[xX] HexDigits_opt . HexDigits BinaryExponent FloatTypeSuffix_opt
		        "(0[xX]" + HexDigits + "?(\\.)" + HexDigits + ")" +

		        ")[pP][+-]?" + Digits + "))" +
		             "[fFdD]?))" +
		             "[\\x00-\\x20]*");// Optional trailing "whitespace"
		            
		  if (Pattern.matches(fpRegex, s)){
		           // Double.valueOf(myString); // Will not throw NumberFormatException
			  return true;
		  }
		  return false;
	}
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}
		return true;
	}
	public static void checkColorMode(ImagePlus image){
	
		int bitDepth = image.getBitDepth();
		//if(bitDepth == ImagePlus.COLOR_256 || bitDepth == ImagePlus.COLOR_RGB || bitDepth == ImagePlus.GRAY32){
		System.out.println(bitDepth);
		//System.out.println(ImagePlus.GRAY16);
		if(bitDepth == 8 || bitDepth == 16){
			//currentWorkImage.setProcessor(((ImageProcessor)(img.getProcessor().clone())));
		}else{
			image.setProcessor(((ImageProcessor)(image.getProcessor().clone())).convertToByteProcessor());
		}
		bitDepth = image.getProcessor().getBitDepth();
		assert bitDepth == 8 || bitDepth == 16;
	}
	public static ImageProcessor cloneProcessor(ImagePlus img){
		ImageProcessor proc = img.getProcessor();
		ImageProcessor originalProcessor = img.getBitDepth() == 16 ? new ShortProcessor(proc.getWidth(), proc.getHeight()) : new ByteProcessor(proc.getWidth(), proc.getHeight()) ;
		for(int i = 0; i < proc.getPixelCount(); i++){
			originalProcessor.set(i, proc.get(i));
		}
		return originalProcessor;
	}
}
/**
 * 
 * @author Olga
 *
 *Main aplication's Window that implements the logic of MTF Plugin 
 *
 */
class TestFrame extends Frame{
	private ArrayList<Controller> controllers = new ArrayList<Controller>();
	private Panel centralPanel; 
	private ArrayList<ParticlePanel> particles = new ArrayList<ParticlePanel>();
	private Image iconImage = new ImageIcon(this.getClass().getResource("/star.png")).getImage();
	
	private Dialog dialog;
	{
		if(iconImage != null) this.setIconImage(iconImage);
		this.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
				dispose();
		       // System.exit(0);
			}
		});
		centralPanel = new Panel();
		this.add(BorderLayout.CENTER, centralPanel);
		controllers.add(new MTFController(this));
		controllers.add(new StarMTFController(this));
		this.setBackground(Color.GRAY);
	}
	
	TestFrame(){}
	
	TestFrame(String title){super(title);}
	
	TestFrame(GraphicsConfiguration gc){super(gc);}
	
	TestFrame(String title, GraphicsConfiguration gc){super(title, gc);}
	
	/** 
	 * add a new Particle Panel to the aplication's Frame
	 * @param pt - ParticlePanel to be added
	 */
	void addParticle(ParticlePanel pt){
		particles.add(pt);
	}
	
	/**
	 * displays the MTF Plugin Window with all previously added Particle Panels
	 */
	void display(){
		IJ.getInstance().setLocation(0, 0);
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		cnst.anchor = GridBagConstraints.WEST;
		cnst.fill = GridBagConstraints.HORIZONTAL;
		cnst.weightx = 1;
		cnst.weighty = 0;
		cnst.gridx = 1;
		int border = 1;
		//centralPanel.setLayout(new GridLayout(particles.size(), 1));
		centralPanel.setLayout(gbl);
		for(ParticlePanel pt : particles){
			if(particles.indexOf(pt) == 0){
				cnst.insets = new Insets(border, border, 0, border);
			}else cnst.insets = new Insets(border, border, border, border);
			cnst.gridy = particles.indexOf(pt);
			gbl.setConstraints(pt, cnst);
			centralPanel.add(pt);
		}
		//this.setMinimumSize(new Dimension(200, 200));
		//System.out.print(this.getLayout() instanceof BorderLayout);
		//if(this.getLayout() instanceof BorderLayout)((BorderLayout)this.getLayout()).setVgap(0);
		
		this.validate();
		this.pack();
		this.setLocation(0, IJ.getInstance().getHeight());//centralize
		if(!this.isVisible())this.setVisible(true);
		
	}

	void showInstructionsDialog(String instructions){
		if(dialog != null){
			dialog.dispose();
		}
		dialog = new Dialog(this);
		Panel instructionsPanel = new Panel(new FlowLayout(FlowLayout.CENTER));
		instructionsPanel.add(new Label(instructions));
		dialog.add(BorderLayout.CENTER, instructionsPanel);
		Button okBtn = new Button("OK");
		Panel btnPanel = new Panel(new FlowLayout(FlowLayout.RIGHT));
		btnPanel.add(okBtn);
		okBtn.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				dialog.dispose();
			}
		});
		okBtn.addKeyListener(new KeyAdapter(){
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER){
					dialog.dispose();
				}
			}
		});
		dialog.addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent e) {
		        dialog.dispose();
			}
		});
		dialog.add(BorderLayout.SOUTH, btnPanel);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setAlwaysOnTop(true);
		dialog.setVisible(true);
	}

	public void hideInstructions() {
		if(dialog != null){
			dialog.dispose();
		}
		
	}
}
interface Observer{
	
	void update(Observable observable);
}
class CalibrationDetails extends Frame implements Observer{
	enum Measure{
		GRAY ("Gray Values"),
		OPTICAL_DENSITY ("Optical Density"),
		REFLECTANCE ("Reflectance");
		
		private final String text;
		
		 private Measure(final String text) {
		    this.text = text;
		 }
		 @Override
		 public String toString() {
		   return text;
		 }
	}
	public static final String ALTERNATIVES = "Test chart TE253";
	public static final String CANCEL = "Cancel";
	public static final String CALIBRATE = "Calibrate";
	public static double [] defaultShortValues = {65535, 60947, 57015, 54428, 47840, 43908, 39321, 34733, 30801, 26214, 21626, 17694, 13107, 8519, 4587, 1310};
	public static double [] defaultByteValues = {255, 237, 222, 204, 186, 170, 153, 135, 120, 103, 84, 69, 51, 33, 18, 5};
	private Label selectedValuesLabel;
	private Checkbox opticalD;
	private Checkbox reflexion;
	private Checkbox gray;
	private TextArea selectedValues;
	private TextArea userDefinedValues;
	private Button cancel;
	private Button alternativesButton;
	private Button calibrate;
	
	//private Listener listener;
	
	CalibrationDetails(OECFAdjustmentListener mainListener){
		super("Calibration Details");
		this.addWindowListener((OECFAdjustmentListener)mainListener);
		this.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
		this.setBackground(new Color(0xf9f9f9));
		Label description1 = new Label("Select a ROI and confirm it with Enter. Measured");
		Label description2 = new Label("values will be displayed in the left column. Write");
		Label description3 = new Label("down your reference values in to the right column.");
		CustomUtils.setDescriptionStyle(description1);
		CustomUtils.setDescriptionStyle(description2);
		CustomUtils.setDescriptionStyle(description3);
		
		Label leftColumnTitle = new Label("MEASURED VALUES:");
		Label rightColumnTitle = new Label("REFERENCE VALUES:");
		//leftColumnTitle.setBackground(new Color(0xffffff));
		
		selectedValuesLabel = new Label(Measure.GRAY.toString());
		
		CheckboxGroup group = new CheckboxGroup(); 
		opticalD = new Checkbox(Measure.OPTICAL_DENSITY.toString(), group, false);
		reflexion = new Checkbox(Measure.REFLECTANCE.toString(), group, false);
		gray = new Checkbox(Measure.GRAY.toString(), group, true);
		//Panel groupContainer = new Panel(new GridLayout(3, 1));
		//groupContainer.add(gray);
		//groupContainer.add(opticalD);
		//groupContainer.add(reflexion);
		Panel p1 = new Panel(new GridBagLayout());
		p1.add(selectedValuesLabel);
		
		p1.setBackground(new Color(0xdfdfdf));
		gray.setBackground(new Color(0xdfdfdf));
		opticalD.setBackground(new Color(0xdfdfdf));
		reflexion.setBackground(new Color(0xdfdfdf));
		
		selectedValues = new TextArea("", 16, 15, TextArea.SCROLLBARS_NONE);
		userDefinedValues = new TextArea("", 16, 15, TextArea.SCROLLBARS_NONE);
		
		cancel = new Button(CalibrationDetails.CANCEL);
		alternativesButton = new Button(CalibrationDetails.ALTERNATIVES);
		calibrate = new Button(CalibrationDetails.CALIBRATE);
		
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		Panel container = new Panel(layout);
		this.add(container);
		
		
		cnst.gridx = 0;
		cnst.gridy = 0;
		cnst.gridwidth = 2;
		cnst.anchor = GridBagConstraints.WEST;
		cnst.insets = new Insets(0, 0, -4, 0);
		layout.setConstraints(description1, cnst);
		container.add(description1);
		
		cnst.gridx = 0;
		cnst.gridy = 1;
		cnst.gridwidth = 2;
		cnst.anchor = GridBagConstraints.WEST;
		cnst.insets = new Insets(0, 0, -4, 0);
		layout.setConstraints(description2, cnst);
		container.add(description2);
		
		cnst.gridx = 0;
		cnst.gridy = 2;
		cnst.gridwidth = 2;
		cnst.anchor = GridBagConstraints.WEST;
		cnst.insets = new Insets(0, 0, 10, 0);
		layout.setConstraints(description3, cnst);
		container.add(description3);
		
		cnst.gridx = 0;
		cnst.gridy = 3;
		cnst.gridwidth = 1;
		cnst.anchor = GridBagConstraints.CENTER;
		cnst.insets = new Insets(0, 0, 0, 0);
		layout.setConstraints(leftColumnTitle, cnst);
		container.add(leftColumnTitle);
		
		cnst.gridx = 1;
		cnst.gridy = 3;
		cnst.gridwidth = 1;
		cnst.anchor = GridBagConstraints.CENTER;
		cnst.insets = new Insets(0, 0, 0, 0);
		layout.setConstraints(rightColumnTitle, cnst);
		container.add(rightColumnTitle);
		
		cnst.gridx = 0;
		cnst.gridy = 4;
		cnst.gridwidth = 1;
		cnst.gridheight = 3;
		//cnst.anchor = GridBagConstraints.CENTER;
		cnst.weightx = 10;
		cnst.insets = new Insets(0, 0, 0, 1);
		cnst.fill = GridBagConstraints.BOTH;
		
		layout.setConstraints(p1, cnst);
		container.add(p1);
		
		cnst.insets = new Insets(0, 1, 0, 0);
		cnst.gridx = 1;
		cnst.gridheight = 1;
		layout.setConstraints(gray, cnst);
		container.add(gray);
		
		cnst.gridy = 5;
		layout.setConstraints(reflexion, cnst);
		container.add(reflexion);
		
		cnst.gridy = 6;
		layout.setConstraints(opticalD, cnst);
		container.add(opticalD);
		
		cnst.weightx = 0;
		cnst.insets = new Insets(2, 0, 0, 0);
		cnst.gridx = 0;
		cnst.gridy = 7;
		layout.setConstraints(selectedValues, cnst);
		container.add(selectedValues);
		
		cnst.gridx = 1;
		layout.setConstraints(userDefinedValues, cnst);
		container.add(userDefinedValues);
		
		cnst.insets = new Insets(0, 0, 0, 0);
		cnst.gridy = 8;
		cnst.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(alternativesButton, cnst);
		container.add(alternativesButton);
		
		cnst.insets = new Insets(30, 0, 0, 0);
		cnst.fill = GridBagConstraints.NONE;
		cnst.gridx = 0;
		cnst.gridy = 9;
		cnst.anchor = GridBagConstraints.WEST;
		layout.setConstraints(cancel, cnst);
		container.add(cancel);
		
		cnst.gridx = 1;
		cnst.anchor = GridBagConstraints.EAST;
		layout.setConstraints(calibrate, cnst);
		container.add(calibrate);
		
		//********************LISTENERS**********************************
		
		cancel.addActionListener((OECFAdjustmentListener)mainListener);
		assert mainListener != null;
		
		CalibrationDetailsListener listener = mainListener.getCalibrationDetailsListener(this);
		assert listener != null;
		
		opticalD.addItemListener(listener);
		reflexion.addItemListener(listener);
		gray.addItemListener(listener);
		
		alternativesButton.addActionListener(listener);
		calibrate.addActionListener(listener);
		
		//IMAGE LISTENERS for Images opened before CalibrationDetailTracker initialization 
		if(WindowManager.getImageCount() > 0){
			for(int imageID : WindowManager.getIDList()){
				WindowManager.getImage(imageID).getCanvas().addMouseListener(listener);
				WindowManager.getImage(imageID).getCanvas().addKeyListener(listener);
				//System.out.println("MouseListener " + calibrationDetailsListener +" for image " + imageID + " added");
				//System.out.println(listener == (CalibrationDetailsListener)(controller.getListener(this)));
			}
		}
		//Listener for Images that will be opened after CalibrationDetailTracker initialization
		ImagePlus.addImageListener(listener);
		//***************************************************************

	}

	public void display(){
		this.validate();
		this.pack();
		this.setLocationRelativeTo(null);//centralize
		if(!this.isVisible())this.setVisible(true);
	}
	@Override
	public void update(Observable observable) {
		if(observable.getOperation() == Observable.PAINT){
			if(!selectedValues.getText().isEmpty()){
				selectedValues.append("\n" + ((CalibrationDetailsListener)observable).getCurrentColorValue());
			}else{
				selectedValues.append("" + ((CalibrationDetailsListener)observable).getCurrentColorValue());
			}
			selectedValues.repaint();
		}else if(observable.getOperation() == Observable.CLOSE){
			this.dispose();
		}else if(observable.getOperation() == Observable.MATCH_GUI){
			gray.setState(true);
			userDefinedValues.setText("");
		}else if(observable.getOperation() == Observable.DEFAULT){
			double [] values = WindowManager.getCurrentImage().getBitDepth() == 16 ? CalibrationDetails.defaultShortValues : CalibrationDetails.defaultByteValues;
			for(Double val : values){
				if(userDefinedValues.getText().isEmpty()){
					userDefinedValues.setText(val+"");
				}
				else{
					userDefinedValues.setText(userDefinedValues.getText() + "\n" + val);
				}
			}
		}else if(observable.getOperation() == Observable.CALIBRATED){
			this.dispose();
		}
		
	}
	public TextArea getSelectedValues(){
		return selectedValues;
	}
	public TextArea getUserDefinedValues(){
		return userDefinedValues;
	}
}
class CalibrationOverlay extends ImagePlus implements Observer{
	private ImagePlus calibrationImage;
	private Overlay overlay;
	private Overlay originalOverlay;
	private Roi[] originalRois;
	
	CalibrationOverlay(ImagePlus calibrationImage){
		this.calibrationImage = calibrationImage;
		//System.out.println("New Calibration overlay " + this.getID() + " for image " + calibrationImage + " is created");
	}
	public ImagePlus getCalibrationImage(){
		return calibrationImage;
	}
	@Override
	public void update(Observable observable) {
		if(observable.getOperation() == Observable.PAINT){
			//System.out.println("Observable " + observable);
			//System.out.println("roi in update : "+ calibrationImage.getRoi());
			Roi roi = (Roi)calibrationImage.getRoi().clone();
			calibrationImage.deleteRoi();
			roi.setStrokeColor(Color.BLUE);
			roi.setStrokeWidth(1);
			
			if(overlay == null){
				originalOverlay = calibrationImage.getOverlay();
				if(originalOverlay != null){
					overlay = originalOverlay;
					originalRois = originalOverlay.toArray();
				}else{
					overlay = new Overlay();
					calibrationImage.setOverlay(overlay);
				}
				
			}
			overlay.add(roi);
			CustomUtils.resetRoiColor();
		}
		else if(observable.getOperation() == Observable.CLOSE){
			//System.out.println("In close");
			overlay.clear();
			//calibrationImage.repaintWindow();
			ImageProcessor originalProcessor = ((CalibrationDetailsListener)observable).getOriginalProcessor();
			//System.out.println("Original image after CLOSE = " + originalImage.getID());
			assert calibrationImage.getProcessor() != originalProcessor;
			calibrationImage.setProcessor(originalProcessor);
			//ImageWindow window = calibrationImage.getWindow();
			//calibrationImage.setProcessor(originalImage.getProcessor());
			//new ImagePlus("reopened original image", originalProcessor).show();
			//assert originalOverlay.toArray().length == 2;
			if(originalRois != null && originalRois.length > 0){
				for(Roi roi : originalRois){
					originalOverlay.add(roi);
				}
				calibrationImage.setOverlay(originalOverlay);
			}
			calibrationImage.deleteRoi();
			calibrationImage.repaintWindow();
		}else if(observable.getOperation() == Observable.CALIBRATED){
			calibrationImage.repaintWindow();
			//System.out.println("Image repainted");
		}		
	}
	
}
/**
 * 
 * @author Olga
 * 
 * A Particle Panel represents one independent Unit which serves the purposes 
 * of specific calculations. Each Particle Panel contains a bunch of user 
 * definable conditions and presets and one CalculateButton, which initiates 
 * the Process of calculation on the basis of adjusted by user values.   
 *
 */
class ParticlePanel extends Panel implements Observer{
	public static final String CALC_MTF = "Calculate EdgeMTF";
	public static final String CALC_STARMTF = "Calculate StarMTF";
	
	private Controller controller;
	private String name;
	private Panel adjustmentsContainer;
	private Button calculateBtn;
	private Panel container; 
	private Map<AdjustmentPanel, ArrayList<Integer>> adjustments = new HashMap<AdjustmentPanel, ArrayList<Integer>>();
	/**
	 * 
	 * @param name - the name of the Operation calculated by this Unit
	 */
	ParticlePanel(String name, Controller controller){
		
		this.name = name;
		this.controller = controller;
		this.calculateBtn  = new Button(name);
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
		container = new Panel(new BorderLayout());
		this.add(container);
		Panel flowPanel = new Panel( new FlowLayout(FlowLayout.CENTER));
		flowPanel.add(calculateBtn);
		container.add(BorderLayout.SOUTH, flowPanel);
		this.setBackground(Color.WHITE);
		calculateBtn.addMouseListener(controller.getParticleListener(this));
	}
	void addAdjustment(AdjustmentPanel adjustment, int x, int y, int gridwidth, int gridheight){
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(x);
		list.add(y);
		list.add(gridwidth);
		list.add(gridheight);
		adjustments.put(adjustment, list);
	}
	void displayAdjustments(){
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		cnst.anchor = GridBagConstraints.WEST;
		cnst.weightx = 1;
		cnst.weighty = 0;
		adjustmentsContainer = new Panel(gbl);
		for(AdjustmentPanel adjustment : adjustments.keySet()){
			cnst.gridx = adjustments.get(adjustment).get(0);
			cnst.gridy = adjustments.get(adjustment).get(1);
			cnst.gridwidth=adjustments.get(adjustment).get(2);
			cnst.gridheight=adjustments.get(adjustment).get(3);
			gbl.setConstraints(adjustment, cnst);
			adjustmentsContainer.add(adjustment);
		}
		container.add(BorderLayout.CENTER, adjustmentsContainer);
	}

	/**
	 * 
	 * @return - a list of userdefined Adjustments
	 */
	public Map<AdjustmentPanel, ArrayList<Integer>> getAjustments(){
		return adjustments;
	}
	public String getName(){
		return this.name;
	}
	@Override
	public void update(Observable observable) {
		// TODO Auto-generated method stub
		
	}
}

abstract class AdjustmentPanel extends Panel implements Observer{
	public AdjustmentPanel() {
		//this.controller = controller;
		this.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 10));
	}
}

////////////////////////////////////////////////////////////////////
/////        MTF ADJUSTMENT PANELS                             /////
////////////////////////////////////////////////////////////////////
class RoiAdjustment extends AdjustmentPanel{
	//CHECK BOX GROUP
	public final static String HORISONTAL_ANGLE = "Horizontal";
	public final static String VERTICAL_ANGLE = "Vertical";
	//CHOICE
	public final static String SINGLE = "Single ROI";
	public final static String MULTIPLE = "Multiple ROI";
	//public final static String BINARY_STAR = "Binary Star";
	
	        boolean 	  multiple = false;
	private Choice 		  select;
	private CheckboxGroup angleOrientation;
	        Checkbox	  horizontal;
	        Checkbox	  vertical;
	private Overlay		  overlay;
	private Color		  color;
	
	RoiAdjustment(Controller controller){
		select = new Choice();
		select.add(RoiAdjustment.SINGLE);
		select.add(RoiAdjustment.MULTIPLE);
		//select.add(RoiAdjustment.BINARY_STAR);
		angleOrientation = new CheckboxGroup();
	
		Panel pn = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		horizontal = new Checkbox(RoiAdjustment.HORISONTAL_ANGLE, angleOrientation, false);
		vertical = new Checkbox(RoiAdjustment.VERTICAL_ANGLE, angleOrientation, true);
		Label label = new Label("Edge:");
		pn.add(label);
		pn.add(horizontal);
		pn.add(vertical);
		
		CustomUtils.setDescriptionStyle(label);
		Panel selectPanel = new Panel(new GridLayout(2, 1));
		selectPanel.add(select);
		//selectPanel.add(label); 
		selectPanel.add(pn); 
		this.add(selectPanel);
//	LISTENERS	*****************************************************************************	
		RoiAdjustmentListener listener = (RoiAdjustmentListener)controller.getListener(this);
		//Choice Listener 
		select.addItemListener(listener);
		
		//Checkbox Listener
		horizontal.addItemListener(listener);
		vertical.addItemListener(listener);
		
		//Listener for Images that will be opened after MTFPlugin initialization
		ImagePlus.addImageListener(listener);
		//System.out.println("ImageOpenedListener : Listener for Images that will be opened after MTFPlugin initialization added");
		
		//IMAGE LISTENERS for Images opened before MTFPlugin initialization 
		if(WindowManager.getImageCount() > 0){
			for(int imageID : WindowManager.getIDList()){
				//openedImagesID.add(imageID);
				ImagePlus imp = WindowManager.getImage(imageID);
				imp.getCanvas().addMouseListener(listener);
				imp.getCanvas().addKeyListener(listener);
				WindowManager.getImage(imageID).getWindow().addWindowFocusListener(listener);
			}
		}

		
// End of LISTENERS *************************************************************************
	}
	@Override
	public void update(Observable observable) {
		RoiAdjustmentListener listener = (RoiAdjustmentListener)observable;
		if(listener.getOperation() == Observable.DELETE){
			if(WindowManager.getCurrentImage().getOverlay() != null)WindowManager.getCurrentImage().getOverlay().clear();
			//if(overlay != null) overlay.clear();
			if(WindowManager.getCurrentImage().getRoi() != null)WindowManager.getCurrentImage().deleteRoi();
			WindowManager.getCurrentImage().draw();
		}else if(listener.getOperation() == Observable.CLEAR){
			if(WindowManager.getCurrentImage().getOverlay() != null)WindowManager.getCurrentImage().getOverlay().clear();
			//if(overlay != null) overlay.clear();
			//WindowManager.getCurrentImage().draw();
		}else if(listener.getOperation() == Observable.PAINT){
			ImageStateTracker tracker = listener.getTracker(WindowManager.getCurrentImage().getID());
			ImagePlus currentImage = WindowManager.getCurrentImage();
			if(currentImage.getOverlay() == null){
				overlay = new Overlay();
				currentImage.setOverlay(overlay);
			}
			else {
				overlay = currentImage.getOverlay();
				overlay.clear();
			}
			assert currentImage.getRoi() == null;
			//prepare for painting all rois, associated with current image
			if(!tracker.getRoiList().isEmpty()){
				for(TreeMap<String, Roi> pair : tracker.getRoiList()){
					for(String key : pair.keySet()){
						Roi currentRoi = (Roi) pair.get(key).clone();
						color = key == RoiAdjustment.HORISONTAL_ANGLE ? Color.GREEN : Color.RED;
						currentRoi.setStrokeColor(color);
						currentRoi.setStrokeWidth(1);
						overlay.add(currentRoi);
					}
				}
			}
			CustomUtils.resetRoiColor();
			
		}else if(listener.getOperation() == Observable.RESET_GUI){
			select.select(RoiAdjustment.SINGLE);
			select.repaint();
			angleOrientation.setSelectedCheckbox(vertical);
			vertical.repaint();
			horizontal.repaint();
			
		}else if(listener.getOperation() == Observable.MATCH_GUI){
			if(WindowManager.getCurrentImage() != null && listener.getTracker(WindowManager.getCurrentImage().getID()) != null){
				ImageStateTracker tracker = listener.getTracker(WindowManager.getCurrentImage().getID());
				select.select(tracker.getSelectionType());
				select.repaint();
				if(tracker.getAngleOrientation() == RoiAdjustment.HORISONTAL_ANGLE){
					angleOrientation.setSelectedCheckbox(horizontal);
				}else {
					angleOrientation.setSelectedCheckbox(vertical);
				}
				vertical.repaint();
				horizontal.repaint();
			}
		}		
	}	
}
class LPAdjustment extends AdjustmentPanel{
	public static final String SELECT_LP = "SelectLP";
	
	public static final String LP_PH = "LP/PH";
	public static final String LP_MM = "LP/MM";
	public static final String  LP_P = "LP/P";
	
	public static final String SELECT_SIZE = "SelectSize";
	
	public static final String  MM = "mm";
	public static final String MKM = "mkm";
	public static final String  NM = "nm";
	
	private Choice    selectLP;
	private Choice    selectSize;
	private TextField pixelAdjustment;
	
	LPAdjustment(Controller controller) {
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		Panel container = new Panel(layout);
		this.add(container);
		
		selectLP = new Choice();
		selectLP.setName(LPAdjustment.SELECT_LP);
		selectLP.add(LPAdjustment.LP_PH);
		selectLP.add(LPAdjustment.LP_MM);
		selectLP.add(LPAdjustment.LP_P);
		//cnst.gridheight = 2;
		
		cnst.gridwidth = 2;
		cnst.fill = GridBagConstraints.HORIZONTAL;
		cnst.insets = new Insets(0, 0, 5, 0);
		cnst.gridx = 0;
		cnst.gridy = 0;
		layout.setConstraints(selectLP, cnst);
		container.add(selectLP);
		
		Label label = new Label("Pixel Size:");
		CustomUtils.setDescriptionStyle(label);
		cnst.anchor = GridBagConstraints.WEST;
		cnst.insets = new Insets(0, 0, 0, 0);
		cnst.gridwidth = 1;
		cnst.gridx = 0;
		cnst.gridy = 1;
		cnst.ipadx = -5;
		layout.setConstraints(label, cnst);
		container.add(label);
		
		Panel pn = new Panel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pixelAdjustment = new TextField("1.0", 2);
		pixelAdjustment.setEnabled(false);
		pn.add(pixelAdjustment);
		selectSize = new Choice();
		selectSize.setName(LPAdjustment.SELECT_SIZE);
		selectSize.add(LPAdjustment.MM);
		selectSize.add(LPAdjustment.MKM);
		selectSize.add(LPAdjustment.NM);
		selectSize.setEnabled(false);
		pn.add(selectSize);
		cnst.ipadx = 0;
		cnst.gridx = 1;
		cnst.gridy = 1;
		layout.setConstraints(pn, cnst);
		container.add(pn);
		
		this.add(container);
		
		LPAdjustmentListener listener = (LPAdjustmentListener)controller.getListener(this);
		
		selectLP.addItemListener(listener);
		selectSize.addItemListener(listener);
		pixelAdjustment.addActionListener(listener);
		pixelAdjustment.addFocusListener(listener);
	}
	@Override
	public void update(Observable observable) {
		LPAdjustmentListener listener = (LPAdjustmentListener)observable;
		if(selectLP.getSelectedItem() == LPAdjustment.LP_MM){
			pixelAdjustment.setEnabled(true);
			selectSize.setEnabled(true);
		}
		else {
			pixelAdjustment.setText("1.0");
			selectSize.select(LPAdjustment.MM);
			pixelAdjustment.setEnabled(false);
			selectSize.setEnabled(false);
		}
		
		if(listener.getOperation() == Observable.RESET_GUI){
			pixelAdjustment.setText("1.0");
			selectLP.select(LPAdjustment.LP_PH);
			selectSize.select(LPAdjustment.MM);
			pixelAdjustment.setEnabled(false);
			selectSize.setEnabled(false);
		}else if(listener.getOperation() == Observable.DEFAULT){
			pixelAdjustment.setText("1.0");
		}	
	}
}
class DeltaSAdjustment extends AdjustmentPanel{
	private TextField deltaSAdjustment;
	DeltaSAdjustment(Controller controller) {
		deltaSAdjustment = new TextField("1.0", 2);
		Label label = new Label("Delta S");
		Label constrain = new Label("(0.0 - 1.0)");
		CustomUtils.setDescriptionStyle(label);
		CustomUtils.setDescriptionStyle(constrain);
		GridBagLayout layout = new GridBagLayout();
		Panel container = new Panel(layout);
		this.add(container);
		GridBagConstraints cnst = new GridBagConstraints();
		cnst.ipady = -10;
		cnst.gridx = 0;
		cnst.gridy = 0;
		cnst.ipadx = -5;
		layout.setConstraints(label, cnst);
		container.add(label);
		cnst.gridy = 1;
		layout.setConstraints(constrain, cnst);
		container.add(constrain);
		cnst.ipadx = 0;
		cnst.ipady = 0;
		cnst.insets = new Insets(2, 0, 2, 15);
		cnst.gridx = 1;
		cnst.gridy = 0;
		cnst.gridheight = 2;
		layout.setConstraints(deltaSAdjustment, cnst);
		container.add(deltaSAdjustment);
		
		
		DeltaSAdjustmentListener listener = (DeltaSAdjustmentListener)controller.getListener(this);
		
		deltaSAdjustment.addActionListener(listener);
		deltaSAdjustment.addFocusListener(listener);
	}

	@Override
	public void update(Observable observable) {
		DeltaSAdjustmentListener listener = (DeltaSAdjustmentListener)observable;
		if(listener.getOperation() == Observable.RESET_GUI){
			deltaSAdjustment.setText("1.0");
		}
	}
}

class LSFAdjustment extends AdjustmentPanel{
	private Checkbox select;
	public LSFAdjustment(Controller controller) {
		select = new Checkbox("LSF Smoothing Filtering");
		this.add(select);
	}
	@Override
	public void update(Observable observable) {
		// TODO Auto-generated method stub
		
	}
}

class WindowAdapter implements WindowListener{
	@Override
	public void windowClosing(WindowEvent e) {}
	@Override
	public void windowActivated(WindowEvent arg0) {}
	@Override
	public void windowClosed(WindowEvent arg0) {}
	@Override
	public void windowDeactivated(WindowEvent arg0) {}
	@Override
	public void windowDeiconified(WindowEvent arg0) {}
	@Override
	public void windowIconified(WindowEvent arg0) {}
	@Override
	public void windowOpened(WindowEvent arg0) {}
}
///////////////////////////////////////////////////////////
///           STAR MTF ADJUSTMENTS PANEL                ///
///////////////////////////////////////////////////////////
class StarTypeAdjustment extends AdjustmentPanel{
	public static final String HARMONY = "Harmonic Star";
	public static final String BINARY = "Binary Star";
	
	private Choice typeSelect;
	
	StarTypeAdjustment(Controller controller){
		StarTypeAdjustmentListener listener = (StarTypeAdjustmentListener)controller.getListener(this);
		typeSelect = new Choice();
		typeSelect.add(StarTypeAdjustment.HARMONY);
		typeSelect.add(StarTypeAdjustment.BINARY);
		
		typeSelect.addItemListener(listener);
		this.add(typeSelect);
	}
	@Override
	public void update(Observable observable) {	
		if(observable.getOperation() == Observable.RESET_GUI){
			typeSelect.select(StarTypeAdjustment.HARMONY);
		}
	}
	
}
class OECFAdjustment extends AdjustmentPanel{
	private Checkbox checkbox;
	OECFAdjustment(Controller controller){
		OECFAdjustmentListener listener = (OECFAdjustmentListener)controller.getListener(this);
		checkbox = new Checkbox("OECF");
		checkbox.addItemListener(listener);
		this.add(checkbox);
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		cnst.anchor = GridBagConstraints.EAST;
		this.setLayout(layout);
		layout.setConstraints(checkbox, cnst);
		
	}
	@Override
	public void update(Observable observable) {
		if(observable.getOperation() == Observable.RESET_GUI){
			checkbox.setState(false);
		}
	}
	
}
class FrequencyAdjustment extends AdjustmentPanel{
	private Checkbox checkbox;
	public FrequencyAdjustment(Controller controller){
		checkbox = new Checkbox("Frequency");
		checkbox.addItemListener(null);
		this.add(checkbox);
	}
	@Override
	public void update(Observable observable) {
		// TODO Auto-generated method stub
		
	}
	
}
class ImagePanel extends Panel{

	private Image starImage = new ImageIcon(this.getClass().getResource("/star.png")).getImage();

    public void paint(Graphics g) {
        super.paint(g);
        g.drawImage(starImage, 0, 0, null);          
    }
   public Dimension getPreferredSize()
    {
        return new Dimension(starImage.getWidth(this), starImage.getHeight(this));
    }

}
class SectorsAdjustment extends AdjustmentPanel{
	public static final String SINGLE_SECTOR = "1 Sector";
	public static final String   ALL_SECTORS = "8 Sectors";
	
	public static final int CYCLE_144 = 144;
	public static final int  CYCLE_72 = 72;
	public static final int  CYCLE_64 = 64;
	public static final int  CYCLE_32 = 32;
	public static final int  CYCLE_16 = 16;
	
	public static final int SECTOR_1_COLOR = 0x383736;
	public static final int SECTOR_2_COLOR = 0x529a37;
	public static final int SECTOR_3_COLOR = 0x276290;
	public static final int SECTOR_4_COLOR = 0x50012a;
	public static final int SECTOR_5_COLOR = 0xd36e24;
	public static final int SECTOR_6_COLOR = 0xbd292d;
	public static final int SECTOR_7_COLOR = 0xefdf43;
	public static final int SECTOR_8_COLOR = 0xcf4572;
	
	private CheckboxGroup group;
	private Checkbox      singleSector;
	private Checkbox      allSectors;
	private TextField     cyclesNum;
	private Choice        cyclesChoice;
	
	
	
	SectorsAdjustment(Controller controller){
		Panel pn = new Panel(new GridLayout(2, 2));
		
		group = new CheckboxGroup();
		allSectors = new Checkbox(SectorsAdjustment.ALL_SECTORS, group, true);
		allSectors.setName(SectorsAdjustment.ALL_SECTORS);
		singleSector = new Checkbox(SectorsAdjustment.SINGLE_SECTOR, group, false);
		singleSector.setName(SectorsAdjustment.SINGLE_SECTOR);
		cyclesNum = new TextField("", 4);
		Label label = new Label("Cycles");
		CustomUtils.setDescriptionStyle(label);
		Panel text = new Panel();
		text.add(cyclesNum);
		text.add(label);
		cyclesNum.setEnabled(false);
		
		cyclesChoice = new Choice();
		cyclesChoice.add(SectorsAdjustment.CYCLE_144+"");
		cyclesChoice.add(SectorsAdjustment.CYCLE_72+"");
		cyclesChoice.add(SectorsAdjustment.CYCLE_64+"");
		cyclesChoice.add(SectorsAdjustment.CYCLE_32+"");
		cyclesChoice.add(SectorsAdjustment.CYCLE_16+"");
		Label lb = new Label("Cycles");
		CustomUtils.setDescriptionStyle(lb);
		Panel choice = new Panel();
		choice.add(cyclesChoice);
		choice.add(lb);
		
		Panel imgPanel = new Panel();
		imgPanel.add(new ImagePanel());
		
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints cnst = new GridBagConstraints();
		cnst.anchor = GridBagConstraints.WEST;
		cnst.weightx=1;
		cnst.weighty=2;
		pn.setLayout(gbl);
		
		cnst.gridx = 0;
		cnst.gridy = 0;
		gbl.setConstraints(allSectors, cnst);
		pn.add(allSectors);
		cnst.gridx = 1;
		cnst.gridy = 0;
		gbl.setConstraints(choice, cnst);
		pn.add(choice);
		cnst.gridx = 0;
		cnst.gridy = 1;
		gbl.setConstraints(singleSector, cnst);
		pn.add(singleSector);
		cnst.gridx = 1;
		cnst.gridy = 1;
		gbl.setConstraints(text, cnst);
		pn.add(text);
		
		cnst.gridwidth = 2;
		cnst.gridx = 0;
		cnst.gridy = 2;
		gbl.setConstraints(imgPanel, cnst);
		pn.add(imgPanel);
		
		
		SectorsAdjustmentListener listener = (SectorsAdjustmentListener)controller.getListener(this);
		allSectors.addItemListener(listener);
		singleSector.addItemListener(listener);
		cyclesNum.addActionListener(listener);
		cyclesChoice.addItemListener(listener);	
		
		this.add(pn);
	}
	public int getCyclesNum(){
		return  CustomUtils.isInteger(cyclesNum.getText()) ? Integer.parseInt(cyclesNum.getText()) : 0;
	}
	private void toggleEnabledBox(){
		if(allSectors.getState()){
			cyclesNum.setEnabled(false);
			cyclesChoice.setEnabled(true);
		}else{
			cyclesChoice.setEnabled(false);
			cyclesNum.setEnabled(true);
			System.out.println("cyclesNum nabled");
		}
	}
	@Override
	public void update(Observable observable) {
		if(observable.getOperation() == Observable.CLEAR){
			cyclesNum.setText("");
		}
		if(observable.getOperation() == Observable.MATCH_GUI){
			toggleEnabledBox();
		}
		if(observable.getOperation() == Observable.RESET_GUI){
			allSectors.setState(true);
			cyclesChoice.setEnabled(true);
			cyclesChoice.select(SectorsAdjustment.CYCLE_144+"");
			cyclesNum.setText("");
			cyclesNum.setEnabled(false);
		}
	}
	
}

//////////////////////////////////////////////////////////
//          CONTROLLER
//////////////////////////////////////////////////////////
abstract class Controller{
	protected ParticleListener particleListener = null;
	protected TestFrame      mainFrame;
	protected ArrayList<Listener>      listeners = new ArrayList<Listener>();
	//flag important for Image EventListeners
	//helps to trigger appropriate EventListener
	//and signal other Listeners to ignore the event
	private static boolean calibrationIsInProgress = false;
	private static boolean selectingStarIsInProgress = false;
	
	Controller(TestFrame mainFrame){
		this.mainFrame = mainFrame;
	}
	protected TestFrame getMainFrame(){
		return mainFrame;
	}
	public ParticleListener getParticleListener(Observer observer){
		if(particleListener != null){
			return particleListener;
		}else{
			particleListener = new ParticleListener(this, observer);
		}
		return particleListener;
	}
	public void resetParticleListeners(){
		particleListener.reset();
	}
	public void setCalibrationIsInProgress(boolean isInProgress){
		calibrationIsInProgress = isInProgress;
		System.out.println("CalibrationIsInProgress set to " + calibrationIsInProgress);
	}
	public void setSelectingStarIsInProgress(boolean isInProgress){
		selectingStarIsInProgress = isInProgress;
	}
	public boolean calibrationIsInProgress(){
		System.out.println("CalibrationIsInProgress returns " + calibrationIsInProgress);
		return calibrationIsInProgress;
	}
	public boolean selectingStarIsInProgress(){
		return selectingStarIsInProgress;
	}
	protected void resetGUI(){
		System.out.println("Controller - resetGUI");
		for(Listener listener: listeners){
			listener.resetGUI(listener);
		}
	}
	protected void resetListeners(){
		System.out.println("Controller - resetListeners");
		for(Listener listener: listeners){
			listener.resetListener();
		}
	}
	abstract public Listener getListener(Observer observer);
	abstract public ArrayList<Listener> getListenersList();
	abstract protected void proceedData();
}
class MTFController extends Controller{
	private MTFCalculator               mtfCalculator;
	
	MTFController(TestFrame mainFrame){
		super(mainFrame);
		initGUI();
	}
	
	private void initGUI(){
		ParticlePanel mtfParticle = new ParticlePanel(ParticlePanel.CALC_MTF, this);
		mtfParticle.addAdjustment(new RoiAdjustment(this), 0, 0, 2, 1);
		mtfParticle.addAdjustment(new LPAdjustment(this), 0, 1, 2, 1);
		mtfParticle.addAdjustment(new DeltaSAdjustment(this), 0, 2, 1, 1);
		mtfParticle.addAdjustment(new OECFAdjustment(this), 1, 2, 1, 1);
		//mtfParticle.addAdjustment(new LSFAdjustment(this), 0, 4, 1, 1);
		mtfParticle.displayAdjustments();
		mainFrame.addParticle(mtfParticle);
	}
	public Listener getListener(Observer observer){
		Listener listener = null;
		if(listeners != null && !listeners.isEmpty()){
			for(Listener lst : listeners){
				if(lst.getClass().getSimpleName().contains(observer.getClass().getSimpleName())){
					return lst;
				}
			}
		}
		if(observer instanceof RoiAdjustment){
			listener = new RoiAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof LPAdjustment){
			listener = new LPAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof DeltaSAdjustment){
			listener = new DeltaSAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof OECFAdjustment){
			listener = new OECFAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof LSFAdjustment){
			listener = new LSFAdjustmentListener(this, observer);
			listeners.add(listener);
		}	
		return listener;
	}
	public void proceedData() {
		MTFCalculator.Builder bd = new MTFCalculator.Builder(this);
		for(Listener listener : listeners){
			if(listener instanceof RoiAdjustmentListener){
				bd.roiList(((RoiAdjustmentListener)listener).getRoiList());
				bd.imageHeight(((RoiAdjustmentListener)listener).getImageHeight());
			}else if(listener instanceof  LPAdjustmentListener){
				bd.pixelSize(((LPAdjustmentListener)listener).getPixelSize());
				bd.PH(((LPAdjustmentListener)listener).phIsSelected());
			}else if(listener instanceof DeltaSAdjustmentListener){
				bd.quotient(((DeltaSAdjustmentListener)listener).getQuoitient());
				System.out.println("Quotient in proceedData() : " + ((DeltaSAdjustmentListener)listener).getQuoitient());
			}else{
				// TODO OECF and LSF Listeners will be implemented here 
			}
		}
		
		mtfCalculator = bd.build();
		mtfCalculator.calculate();
	}
	public ArrayList<Listener> getListenersList(){
		return listeners;
	}	
}
class StarMTFController extends Controller{ 
	private StarMTFCalculator   starMTFCalculator;
	
	StarMTFController(TestFrame mainFrame) {
		super(mainFrame);
		initGUI();
	}
	private void initGUI() {
		ParticlePanel starMTFParticle = new ParticlePanel(ParticlePanel.CALC_STARMTF, this);
		starMTFParticle.addAdjustment(new StarTypeAdjustment(this), 0, 0, 1, 1);
		starMTFParticle.addAdjustment(new OECFAdjustment(this), 1, 0, 1, 1);
		//starMTFParticle.addAdjustment(new FrequencyAdjustment(this), 1, 1, 1, 1);
		starMTFParticle.addAdjustment(new SectorsAdjustment(this), 0, 2, 2, 0);
		starMTFParticle.displayAdjustments();
		mainFrame.addParticle(starMTFParticle);
		
	}

	public Listener getListener(Observer observer){
		Listener listener = null;
		
		
		if(listeners != null && !listeners.isEmpty()){
			for(Listener lst : listeners){
				if(lst.getClass().getSimpleName().contains(observer.getClass().getSimpleName())){
					return lst;
				}
			}
		}
		if(observer instanceof StarTypeAdjustment){
			listener = new StarTypeAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof OECFAdjustment){
			listener = new OECFAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof FrequencyAdjustment){
			listener = new FrequencyAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		else if(observer instanceof SectorsAdjustment){
			System.out.println(this);
			listener = new SectorsAdjustmentListener(this, observer);
			listeners.add(listener);
		}
		return listener;
	}
	
	@Override
	protected void proceedData() {
		StarMTFCalculator.Builder bd = new StarMTFCalculator.Builder(this);
		for(Listener listener : listeners){
			if(listener instanceof StarTypeAdjustmentListener){
				bd.starType(((StarTypeAdjustmentListener)listener).getStarType());
			}else if(listener instanceof  SectorsAdjustmentListener){
				bd.cyclesNum(((SectorsAdjustmentListener)listener).getCyclesNum());	
				bd.sectorsNum(((SectorsAdjustmentListener)listener).getSectorsNum());
			}else{
				// TODO OECF and LSF Listeners will be implemented here 
			}
		}
		bd.imageHeight(particleListener.getImageHeight());
		bd.starImage(particleListener.getStarImage());
		starMTFCalculator = bd.build();
		starMTFCalculator.calculate();
	}
	@Override
	public ArrayList<Listener> getListenersList() {
		return listeners;
	}

	
}
interface Varificator{
	boolean varify();
}
interface Observable {
	public final static int       NONE = 0;
	public final static int     DELETE = 1;
	public final static int      PAINT = 2;
	public static final int      CLEAR = 3;
	public static final int  RESET_GUI = 4;
	public static final int  MATCH_GUI = 5;
	public static final int      CLOSE = 6;
	public static final int CALIBRATED = 7;
	public static final int    DEFAULT = 8;
	
	void setObserver(Observer observer);
	void notifyObservers();
	int getOperation(); 
	
}
abstract class Listener implements Varificator, Observable{
	protected int currentOperation = Observable.NONE;
	
	protected void resetGUI(Listener listener){
		listener.currentOperation = Observable.RESET_GUI;
		listener.notifyObservers();
		listener.currentOperation = Observable.NONE;
	}
	protected abstract void resetListener();
}

abstract class MTFListener extends Listener{
	
}
abstract class StarMTFListener extends Listener{

}
class ParticleListener implements Varificator, MouseListener, KeyListener{
	private Controller controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	
	private int imageHeight;
	
	private ImagePlus initialStar;
	private ImagePlus starToCalc;
	private ImagePlus starImage; 
	
	ParticleListener(Controller controller, Observer observer) {
		this.controller = controller;
		this.observers.add(observer);
	}
	public int getImageHeight(){
		return imageHeight;
	}
	public ImagePlus getStarImage(){
		return starToCalc;
		//return starImage;
	}
	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		if(controller.getListenersList().isEmpty())return false;
		for(Listener listener : controller.getListenersList()){
			System.out.println(listener.getClass().getCanonicalName());
			readyForCalculation = readyForCalculation && listener.varify();
		}
		return readyForCalculation;
	}
	public void reset(){
		if(initialStar != null){
			initialStar.show();
			initialStar.getWindow().setLocation(controller.getMainFrame().getX() + controller.getMainFrame().getWidth(),controller.getMainFrame().getY());
			
		}
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(((Button)e.getSource()).getLabel() == ParticlePanel.CALC_STARMTF){
			if(WindowManager.getIDList() != null){
				for(int imageID : WindowManager.getIDList()){
					WindowManager.getImage(imageID).getCanvas().addKeyListener(this);
				}
				controller.getMainFrame().showInstructionsDialog("Please, select the region with a star and click ENTER");
				controller.setSelectingStarIsInProgress(true);
			}else{
				controller.getMainFrame().showInstructionsDialog("There is no image opened");
			}
		}
		else if(varify()){
			controller.proceedData();
		}	
	}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER){
			controller.getMainFrame().hideInstructions();
			controller.getMainFrame().showInstructionsDialog("Select the center of the star");
			starImage = ((ImageCanvas)e.getSource()).getImage();
			imageHeight = starImage.getHeight();
			initialStar = new ImagePlus(starImage.getTitle(), CustomUtils.cloneProcessor(starImage));
			//initialStar.hide();
			
			//initialStar.getWindow().setExtendedState(Frame.ICONIFIED);
			//ImagePlus cropedImage = new ImagePlus("Croped image", currentImage.getProcessor().crop());
			starImage.setProcessor(starImage.getProcessor().crop());
			
			//currentImage.changes = false;
			starImage.getCanvas().removeMouseListener(this);
			starImage.getCanvas().setOverlay(null);
			starImage.getWindow().setLocation(controller.getMainFrame().getX() + controller.getMainFrame().getWidth(),controller.getMainFrame().getY());
			starImage.getWindow().toFront();
			starImage.repaintWindow();
			//currentImage.close();
			
			
			//cropedStar.show();
			//cropedImage.show();
			starImage.getCanvas().addMouseListener(new MouseListener(){

				@Override
				public void mouseClicked(MouseEvent et) {
					
					controller.getMainFrame().hideInstructions();
					starImage = ((ImageCanvas)et.getSource()).getImage();
					int pointerX = starImage.getCanvas().offScreenX(et.getX())-14;
			        int pointerY = starImage.getCanvas().offScreenY(et.getY())-14;
			        int centerX = starImage.getCanvas().offScreenX(et.getX());
			        int centerY = starImage.getCanvas().offScreenY(et.getY());
			        System.out.println("X: " + centerX);
			        System.out.println("Y: " + centerY);
			       
			        ArrayList<Integer> sides = new ArrayList<Integer>(); 
			        sides.add(centerX);
			        sides.add(starImage.getWidth()-centerX);
			        sides.add(centerY);
			        sides.add(starImage.getHeight()-centerY);
			        java.util.Collections.sort(sides);
			        
			        int minRadius =sides.get(0);
			        System.out.println(sides);
			        System.out.println(minRadius);
			        System.out.println(minRadius);
			        starImage.setRoi(centerX-minRadius, centerY-minRadius, minRadius*2, minRadius*2);
			        starImage.setProcessor(starImage.getProcessor().crop());
			        starImage.getCanvas().removeMouseListener(this);
			        starToCalc = new ImagePlus("Star, prepared for calculations", CustomUtils.cloneProcessor(starImage));
			        starImage.changes = false;
			        starImage.close();
			        
			        controller.setSelectingStarIsInProgress(false);
			        if(varify()){
						controller.proceedData();
			        }
				}

				@Override
				public void mouseEntered(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseExited(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mousePressed(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseReleased(MouseEvent arg0) {
					// TODO Auto-generated method stub
					
				}
			});
		}
		
	}
	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}

class StarTypeAdjustmentListener extends StarMTFListener implements ItemListener{
	private Controller 	controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	
	private String starType = StarTypeAdjustment.HARMONY;
	
	StarTypeAdjustmentListener(Controller starMTFController,
			Observer observer) {
		controller = starMTFController;
		observers.add(observer);
	}
	
	@Override
	public void setObserver(Observer observer) {}
	@Override
	public void notifyObservers() {
		for(Observer o : observers){
			o.update(this);
		}
	}
	
	@Override
	public int getOperation() {
		return currentOperation;
	}
	
	@Override
	public boolean varify() {return true;}
	@Override
	public void itemStateChanged(ItemEvent e) {
		if(!(WindowManager.getImageCount() > 0)){
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
			controller.getMainFrame().showInstructionsDialog("Please, first open an image!");
		}
		starType = (String)e.getItem();	
		//System.out.println(starType);
	}
	public String getStarType(){
		return starType;
	}

	@Override
	protected void resetListener() {
		starType = StarTypeAdjustment.HARMONY;	
	}
}
class OECFAdjustmentListener extends Listener implements ItemListener, ActionListener, WindowListener{
	private Controller 	        controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	
	private CalibrationDetails  calibrationWindow;
	private CalibrationDetailsListener calibrationDetailsListener;
	
	OECFAdjustmentListener(){}

	OECFAdjustmentListener(Controller starMTFController,
			Observer observer) {
		controller = starMTFController;
		observers.add(observer);
	}
	
	public CalibrationDetailsListener getCalibrationDetailsListener(Observer observer) {
		if(calibrationDetailsListener == null)calibrationDetailsListener = new CalibrationDetailsListener(controller, observer);
		return calibrationDetailsListener;
	}
	
	@Override
	public void setObserver(Observer observer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyObservers() {
		for(Observer o : observers){
			o.update(this);
		}
	}

	@Override
	public int getOperation() {
		return currentOperation;
	}

	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		
		if(controller.calibrationIsInProgress()){
			readyForCalculation = false;
			controller.mainFrame.showInstructionsDialog("Calibration in Progress!");
		}
		
		return readyForCalculation;
	}
	private void removeListeners(){
		//****************REMOVE LISTENERS*******************************
				if(WindowManager.getImageCount() > 0){
					for(int imageID : WindowManager.getIDList()){
						WindowManager.getImage(imageID).getCanvas().removeMouseListener((CalibrationDetailsListener)calibrationDetailsListener);
					}
				}
				ImagePlus.removeImageListener((CalibrationDetailsListener)calibrationDetailsListener);
		//***************************************************************
	}
	protected void cancelCalibration(){
		removeListeners();
		controller.setCalibrationIsInProgress(false);
		calibrationDetailsListener.disposeGUI();
		calibrationWindow.dispose();
		calibrationWindow = null;
		calibrationDetailsListener = null;
		currentOperation = Observable.RESET_GUI;
		notifyObservers();
		
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		if(!(WindowManager.getImageCount() > 0)){
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
			controller.getMainFrame().showInstructionsDialog("Please, first open an image!");
		}else{
			if(e.getStateChange() == ItemEvent.SELECTED){
				controller.setCalibrationIsInProgress(true);
				calibrationWindow = new CalibrationDetails(this);				
				calibrationWindow.display();
				
				//controller.getMainFrame().showInstructionsDialog("Please, select regions for calibration");
			}else{
				cancelCalibration();			
			}
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		cancelCalibration();		
	}

	public void windowClosing(WindowEvent e) {
		cancelCalibration();
	}
	public void windowClosed(WindowEvent e) {}
	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}

	@Override
	protected void resetListener() {
		removeListeners();
		calibrationDetailsListener = null;
	}
}

class CalibrationDetailsListener extends OECFAdjustmentListener implements ItemListener, ImageListener, MouseListener, KeyListener{
	private Controller 	        controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	
	private ImageProcessor     originalProcessor;
	private CalibrationOverlay calibrationOverlay;
	private ImagePlus          calibrationImage;
	private Integer            calibrationImageID = null;
	private String             measure = CalibrationDetails.Measure.GRAY.toString();
	
	private String[]          parsedSelectedValues;
	private String[]          parsedUserDefinedValues;
	private TextArea          selectedValues;
	private TextArea          userDefinedValues;
	private ArrayList<Double> selectedColorValues = new ArrayList<Double>();
	private ArrayList<Double> userDefinedColorValues = new ArrayList<Double>();
	private ArrayList<Double> defaultColorValues = new ArrayList<Double>();
	private String            currentColorValue;
	private int               currentOperation = Observable.NONE;
	private int               bitDepth;
	private DecimalFormat df;
	
	
	CalibrationDetailsListener(Controller starMTFController, Observer observer) {
		controller = starMTFController;
		if(observer != null){observers.add(observer);}
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.GERMAN); 
		otherSymbols.setDecimalSeparator('.'); 
		df = new DecimalFormat("#.00", otherSymbols);
		//System.out.println("CalibrationDetailsListener initialised");
	}

	public void disposeGUI() {
		currentOperation = Observable.CLOSE;
		notifyObservers();
		calibrationOverlay = null;
		calibrationImageID = null;
	}
	
	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		
		for(Observer o: observers){
			if(o instanceof CalibrationDetails){
				CalibrationDetails calibrationWindow = (CalibrationDetails)o;
				TextArea selectedValues = calibrationWindow.getSelectedValues();
				TextArea userDefinedValues = calibrationWindow.getUserDefinedValues();
				if(userDefinedValues.getText().trim().length() == 0){
					readyForCalculation = false;
					controller.getMainFrame().showInstructionsDialog("Please, write down custum values in the right text field!");
				}else{
					String userInput = userDefinedValues.getText();
					userInput = userInput.replaceAll("\\s+", "\t");
					String selectionInput = selectedValues.getText();
					selectionInput = selectionInput.replaceAll("\\s+", "\t");
					String[] parsedUserString = userInput.split("\t");
					String[] parsedSelectionString = selectionInput.split("\t");
					
					int lengthUI = 0;
					int lengthGI = 0;
					if(parsedSelectionString.length < 2){
						readyForCalculation = false;
						controller.getMainFrame().showInstructionsDialog("Please, select more than two ROI for calibration!");
					}else{
						
						for(String s : parsedUserString){
							if(CustomUtils.stringParsableToDouble(s)){	
								lengthUI++;
							}else{
								readyForCalculation = false;
								controller.getMainFrame().showInstructionsDialog("Input must be a number in format xxx.xx");
								break;
							}
						}
						if(readyForCalculation){
							for(String s : parsedSelectionString){
								if(CustomUtils.stringParsableToDouble(s)){	
									lengthGI++;
								}else{
									readyForCalculation = false;
									controller.getMainFrame().showInstructionsDialog("Input must be a number in format xxx.xx");
									break;
								}
							}
						}
					}
					if(readyForCalculation && lengthUI != lengthGI ){
						readyForCalculation = false;
						controller.getMainFrame().showInstructionsDialog("The amount of selected Values must match the amount of defined values!"); 
					}else if(readyForCalculation){
						this.selectedValues = selectedValues;
						this.userDefinedValues = userDefinedValues;
						this.parsedSelectedValues = parsedSelectionString;
						this.parsedUserDefinedValues = parsedUserString;
						readyForCalculation = true;
					}
				}
			}
		}
		
		return readyForCalculation;
	}

	@Override
	public void setObserver(Observer observer) {
		observers.add(observer);		
	}

	@Override
	public void notifyObservers() {
		System.out.println("Observers registered in CalibDetList : "+ observers);
		for(Observer o : observers){
			o.update(this);
		}
	}
	public String getCurrentColorValue(){
		return currentColorValue;
	}
	@Override
	public int getOperation() {
		return currentOperation;
	}

	public ImageProcessor getOriginalProcessor(){
		return originalProcessor;
	}
	public ImagePlus getCalibrationImage(){
		return calibrationImage;
	}
	public int getBitDepth(){
		return bitDepth;
	}
	public ArrayList<Double> getSelectedColorValues(){
		return selectedColorValues;
	}
	public ArrayList<Double> getUserDefinedColorValues(){
		return userDefinedColorValues;
	}
	private void overwork(InputEvent e){
		if (calibrationImageID == null){
			calibrationImageID = ((ImagePlus)((ImageCanvas)e.getSource()).getImage()).getID();
			calibrationImage = WindowManager.getImage(calibrationImageID);
			bitDepth = calibrationImage.getBitDepth();
			System.out.println("bitDepth " + bitDepth);
			//System.out.println("bitDepth != 8 " + (bitDepth != 8));
			//System.out.println("bitDepth != 16 " + (bitDepth != 16));
			if(bitDepth != 8 && bitDepth != 16){
				System.out.println("Processor convertion");
				calibrationImage.setProcessor(calibrationImage.getProcessor().convertToByteProcessor());
				//calibrationImage.getProcessor().getBitDepth();
				bitDepth = calibrationImage.getBitDepth();
				System.out.println(bitDepth);
			}
			
			//assert bitDepth == 8 || bitDepth == 16;
			
			originalProcessor = CustomUtils.cloneProcessor(calibrationImage);
			//System.out.println("Saveing copy of original Image " + originalProcessor.getID());
			calibrationOverlay = new CalibrationOverlay(calibrationImage); 
			this.setObserver(calibrationOverlay);
			//System.out.println("id set");
		}
		if(((ImagePlus)((ImageCanvas)e.getSource()).getImage()).getID() == calibrationImageID){
			System.out.println("id = calibImgID");
			Roi currentRoi = calibrationImage.getRoi();
			if( currentRoi != null){
				//System.out.println("currentRoi " + currentRoi);
				currentOperation = Observable.PAINT;
				
				//TODO Write down roi in selectedColorValues
				Analyzer analyzer = new Analyzer(currentRoi.getImage());
				if(ResultsTable.getResultsTable().getCounter()>0){ResultsTable.getResultsTable().deleteRow(0);}
				analyzer.measure();
				
				//analyzer.displayResults();
				ResultsTable table = ResultsTable.getResultsTable(); 
				currentColorValue = df.format(table.getValueAsDouble(1, 0));
				
				//selectedColorValues.add(currentColorValue);
				notifyObservers();
			}
		}else{
			controller.getMainFrame().showInstructionsDialog("Please, complete the calibration!");
		}
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		/*System.out.println("In MouseListener");
		if(e.getClickCount() == 1){
			int imgID = ((ImagePlus)((ImageCanvas)e.getSource()).getImage()).getID();
			ImagePlus img = WindowManager.getImage(imgID);
			assert img.getOverlay() != null;
			System.out.println(img.getOverlay());
			System.out.println(img.getOverlay().toArray().length);
			System.out.println(img.getOverlay().toArray());
			System.out.println("img.getRoi() " + img.getRoi());
		}*/
		if(e.getClickCount() == 2){
			//System.out.println("In MouseListener");
			overwork(e);
		}
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent arg0) {}
	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyCode() == KeyEvent.VK_ENTER){
			System.out.println("enter");
			overwork(e);
		}
	}
	@Override
	public void keyReleased(KeyEvent arg0) {}
	@Override
	public void keyTyped(KeyEvent arg0) {}

	@Override
	public void imageOpened(ImagePlus imp) {
		if(!(imp.getWindow() instanceof PlotWindow) && (!controller.calibrationIsInProgress() || !controller.selectingStarIsInProgress())){
			//System.out.println("Window opened");
			int imageID = imp.getID();
			
			WindowManager.getImage(imageID).getCanvas().addMouseListener(this);
			//System.out.println("CalibrationDetailsListener: MouseListener " + this + "  for just opened image " + imageID + " removed");
		}
		
	}
	@Override
	public void imageClosed(ImagePlus imp) {
		if(!(imp.getWindow() instanceof PlotWindow)){
			//System.out.println("Window closed");
			//int imageID = imp.getID();
			
			imp.getCanvas().removeMouseListener(this);
			//System.out.println("CalibrationDetailsListener: MouseListener " + this + "  for just opened image " + imageID + " removed");
		}
		
	}
	public void imageUpdated(ImagePlus imp) {}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(((Button)(e.getSource())).getLabel() == CalibrationDetails.CALIBRATE){
			if(varify()){
				writeDownValues();
				MTFCalibrator calibrator = new MTFCalibrator(this);
				calibrationImage = calibrator.calibrate();
				controller.setCalibrationIsInProgress(false);
				currentOperation = Observable.CALIBRATED;
				notifyObservers();
			}
			
			
		}else if(((Button)(e.getSource())).getLabel() == CalibrationDetails.ALTERNATIVES){
			measure = CalibrationDetails.Measure.GRAY.toString();
			currentOperation = Observable.MATCH_GUI;
			notifyObservers();
			double [] values = WindowManager.getCurrentImage().getBitDepth() == 16 ? CalibrationDetails.defaultShortValues : CalibrationDetails.defaultByteValues;
			for( Double val : values){
				defaultColorValues.add(val);
			}
			currentOperation = Observable.DEFAULT;
			notifyObservers();
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent e) {
		measure = ((Checkbox)(e.getSource())).getLabel();	
	}
	
	private void writeDownValues() {
		for(String stringValue: parsedSelectedValues){
			assert !stringValue.isEmpty();
			stringValue = stringValue.contains(".") ? stringValue.substring(0, stringValue.indexOf(".") + 2) : stringValue;
			selectedColorValues.add(Double.valueOf(stringValue));
		}
		for(String stringValue: parsedUserDefinedValues){
			assert !stringValue.isEmpty();
			double value = 0;
			//Pixel Value
			if(measure == CalibrationDetails.Measure.GRAY.toString()){
				value = Double.valueOf(stringValue);
			}
			//OpticalDencity
			else if(measure == CalibrationDetails.Measure.OPTICAL_DENSITY.toString()){
				value = (Math.pow(2, bitDepth) - 1)*Math.pow(10, -value);
			}
			//Reflection
			else if(measure == CalibrationDetails.Measure.REFLECTANCE.toString()){
				value = (Math.pow(2, bitDepth) - 1)*value/100;
			}
			userDefinedColorValues.add(value);
		}
		
	}
	
}
class FrequencyAdjustmentListener extends StarMTFListener{

	FrequencyAdjustmentListener(StarMTFController starMTFController,
			Observer observer) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void setObserver(Observer observer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyObservers() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getOperation() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean varify() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void resetListener() {
		// TODO Auto-generated method stub
		
	}
	
}
class SectorsAdjustmentListener extends StarMTFListener implements ItemListener, ActionListener{
	private Controller 	        controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();

	private int cyclesNum = SectorsAdjustment.CYCLE_144;
	private String sectorsNum = SectorsAdjustment.ALL_SECTORS;
	
	SectorsAdjustmentListener(StarMTFController starMTFController,
			Observer observer) {
		this.controller = starMTFController;
		observers.add(observer);
	}
	public int getCyclesNum(){
		if(sectorsNum == SectorsAdjustment.SINGLE_SECTOR){
			for(Observer o : observers){
				if(o instanceof SectorsAdjustment){
					cyclesNum = ((SectorsAdjustment) o).getCyclesNum();
				}
			}
		}
		return cyclesNum;
	}
	public String getSectorsNum(){
		return sectorsNum;
	}
	@Override
	public void setObserver(Observer observer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyObservers() {
		for(Observer o : observers){
			o.update(this);
		}
	}

	@Override
	public int getOperation() {
		return currentOperation;
	}

	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		if(getCyclesNum() == 0){
			readyForCalculation = false;
			System.out.println(controller);
			controller.getMainFrame().showInstructionsDialog("Cycles amount for a single sector must be specified");
			
		}
		return readyForCalculation;
	}

	@Override
	protected void resetListener() {
		cyclesNum = SectorsAdjustment.CYCLE_144;
		sectorsNum = SectorsAdjustment.ALL_SECTORS;
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		
		if(e.getSource() instanceof Checkbox){
			currentOperation = Observable.MATCH_GUI;
			notifyObservers();
			String eSourceName = (((Checkbox)e.getSource()).getName());
			System.out.println(eSourceName);
			if(eSourceName == SectorsAdjustment.ALL_SECTORS){
				sectorsNum = SectorsAdjustment.ALL_SECTORS;
				cyclesNum = SectorsAdjustment.CYCLE_144;
				System.out.println("SectorsNum : " + sectorsNum + " CyclesNum : " + cyclesNum);
			}else{
				sectorsNum = SectorsAdjustment.SINGLE_SECTOR;
				cyclesNum = 0;
				System.out.println("SectorsNum : " + sectorsNum + " CyclesNum : " + cyclesNum);
			}
		}else if(e.getSource() instanceof Choice){
			Choice select = (Choice)e.getSource();
			if(CustomUtils.isInteger( select.getSelectedItem())){
				cyclesNum = Integer.parseInt(select.getSelectedItem()); 
				System.out.println("SectorsNum : " + sectorsNum + " CyclesNum : " + cyclesNum);
			}
		}
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		((TextField)e.getSource()).transferFocus();
		String input = ((TextField)e.getSource()).getText();
		if(!input.isEmpty()){
			if(CustomUtils.isInteger(input)){
		
				cyclesNum = Integer.valueOf(input);
				System.out.println("!SectorsNum : " + sectorsNum + " !CyclesNum : " + cyclesNum);
			}else{
			    cyclesNum = 0;
				currentOperation = Observable.CLEAR;
				notifyObservers();
				controller.getMainFrame().showInstructionsDialog("Cycles amount must be an integer");
			}
		}else{
			controller.getMainFrame().showInstructionsDialog("Cycles amount must be specified");
		}
		
	}
	
}

class RoiAdjustmentListener extends MTFListener implements ItemListener, MouseListener, ImageListener, KeyListener, WindowFocusListener{
	
	//constants specifying current state of roiManage() operation
	public static final int         FIRST_DEFAULT_ROI_WRITTEN = 1;
	public static final int 		     DEFAULT_ROI_WRITTEN  = 2;
	public static final int       DEFAULT_ROI_ANGLE_REWRITTEN = 3;
	public static final int 	          ROI_DELETED_NOIMAGE = 4;
	public static final int ROI_DELETED_SELECTIONTYPE_CHANGED = 5;
	public static final int		    ROI_DELETED_ANGLE_CHANGED = 6;
	public static final int		      ROI_DELETED_USERCOMMAND = 7;
	public static final int         USERDEFINED_ROI_REWRITTEN = 8;
	public static final int		                         NONE = 9;
	private static final int         RESET_ANGLE_FOR_MULTIPLE = 0;
	
	
	private Controller 		   controller;
	private ArrayList<Observer>     observers = new ArrayList<Observer>();
	private ArrayList<ImageStateTracker> imageTrackList;
	private int                 closedImageID;
    
	//Default flag presets	
	private boolean selectionTypeChanged = false;
	private boolean         angleChanged = false;
	private String      roiSelectionType = RoiAdjustment.SINGLE;
	private String      angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
	
	
	RoiAdjustmentListener(Controller controller, Observer observer){
		this.controller = controller;
		setObserver(observer);
		imageTrackList = new ArrayList<ImageStateTracker>();

		if(WindowManager.getImageCount() > 0){
			for(int imageID : WindowManager.getIDList()){
				//set default adjustments for previously opened images
				roiSelectionType = RoiAdjustment.SINGLE;
				angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
				int operationExecuted = manageRoi(imageID);// 1) DEFAULT ROIS INIT : add initial Roi of an Image, opened before MTF Plugin init, to roiList 
				assert operationExecuted == RoiAdjustmentListener.FIRST_DEFAULT_ROI_WRITTEN || operationExecuted == RoiAdjustmentListener.DEFAULT_ROI_WRITTEN;
				assert imageTrackList.size() > 0;
				assert getTracker(imageID).getImageID() == imageID;
				assert getTracker(imageID).getDefaultRoi().getBounds().getWidth() == WindowManager.getImage(imageID).getWidth();
				assert getTracker(imageID).getDefaultRoi().getBounds().getHeight() == WindowManager.getImage(imageID).getHeight();
				assert getTracker(imageID).getSelectionType() == RoiAdjustment.SINGLE;
				assert getTracker(imageID).getAngleOrientation() == RoiAdjustment.VERTICAL_ANGLE;
				assert getTracker(imageID).getRoiList().isEmpty();
			}
			assert imageTrackList.size() == WindowManager.getImageCount();
		}
	}
	public int getImageHeight() {
		ImagePlus img = WindowManager.getCurrentImage();
		if(img != null){
			return img.getHeight();
		}
		return 0;
	}

	/**
	 * Tracks all the information about user Roi adjustments, necessary for calculation of MTF    
	 * @return static final int - indicates, that a ROI was written, rewritten or deleted 
	 */
	private synchronized int  manageRoi(int imageID){
		Integer currentImageID = null;
		if(WindowManager.getCurrentImage() != null) currentImageID = imageID;
		
		boolean roiIsSelected = false;
		if(currentImageID != null)roiIsSelected = WindowManager.getCurrentImage().getRoi() != null;
		boolean imageTrackListIsEmpty = imageTrackList.isEmpty();
		boolean closedImageIsInTrackList = false;
		int		closedImageIndex = 0;
		boolean imageIsInTrackList = false;
		boolean trackerHasDefaultPresets = false;
		if(!imageTrackListIsEmpty){
			for (ImageStateTracker tracker : imageTrackList){
				if(tracker.getImageID() == closedImageID){
					currentImageID = null;
					closedImageIsInTrackList = true;
					closedImageIndex = imageTrackList.indexOf(tracker);
					break;
				}
				if(tracker.getImageID() == currentImageID){
					imageIsInTrackList = true;	
				}
			}
			if(currentImageID != null && imageIsInTrackList){
				trackerHasDefaultPresets = getTracker(currentImageID).getRoiList().size() == 0;
			}
		}
		
		//*********** 1) DEFAULT ROIS INIT:
		if(imageTrackListIsEmpty){
			ImageStateTracker tracker = new ImageStateTracker(currentImageID);
			imageTrackList.add(tracker);
			return RoiAdjustmentListener.FIRST_DEFAULT_ROI_WRITTEN;
		}
		if(!imageIsInTrackList && !closedImageIsInTrackList){
			ImageStateTracker tracker = new ImageStateTracker(currentImageID);
			imageTrackList.add(tracker);
			return RoiAdjustmentListener.DEFAULT_ROI_WRITTEN;
		}
		if(imageIsInTrackList && trackerHasDefaultPresets && angleChanged){
			getTracker(currentImageID).setAngleOrientation(angleOrientation);
			angleChanged = false;
			return RoiAdjustmentListener.DEFAULT_ROI_ANGLE_REWRITTEN;
		}
		
		//********** 2) DELETE OPERATIONS:
		if(closedImageIsInTrackList){
			imageTrackList.remove(closedImageIndex);
			return RoiAdjustmentListener.ROI_DELETED_NOIMAGE;
		}
		if(selectionTypeChanged || roiSelectionType == RoiAdjustment.SINGLE /*angleChanged/clearBeforeRewrite*/|| currentOperation == Observable.DELETE){
			getTracker(currentImageID).clearRoiList();	
			if(selectionTypeChanged){
				getTracker(currentImageID).setSelectionType(roiSelectionType);
				selectionTypeChanged = false;
				return RoiAdjustmentListener.ROI_DELETED_SELECTIONTYPE_CHANGED;
			}else if(angleChanged){
				getTracker(currentImageID).setAngleOrientation(angleOrientation);
				angleChanged = false;
				return RoiAdjustmentListener.ROI_DELETED_ANGLE_CHANGED;
			}else if(currentOperation == Observable.DELETE){
				roiSelectionType = RoiAdjustment.SINGLE;
				angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
				getTracker(currentImageID).setSelectionType(roiSelectionType);
				getTracker(currentImageID).setAngleOrientation(angleOrientation);
				return RoiAdjustmentListener.ROI_DELETED_USERCOMMAND;
			}else{
				assert currentOperation == Observable.PAINT;
			}
		}
		
		//********* 3) Reset angle for multiple selection type
		if(roiSelectionType == RoiAdjustment.MULTIPLE && angleChanged){
			getTracker(currentImageID).setAngleOrientation(angleOrientation);
			angleChanged = false;
			return RoiAdjustmentListener.RESET_ANGLE_FOR_MULTIPLE;
		}
		
		//********** 4) WRITE OPERATIONS:
		if(roiIsSelected){
			getTracker(currentImageID).addAngleRoiPair(angleOrientation, (Roi) WindowManager.getCurrentImage().getRoi().clone(), roiSelectionType);
			WindowManager.getCurrentImage().deleteRoi();
			return RoiAdjustmentListener.USERDEFINED_ROI_REWRITTEN;
		}
		return RoiAdjustmentListener.NONE;
	}
	
	/**
	 * Constructs the resultant roiList for MTF calculations
	 * The method is used by Controller before Controller tells MTF to calculate the value
	 * @return TreeMap<Integer, ArrayList<TreeMap<String, Roi>>> - resultant roiList
	 */
	@SuppressWarnings("unchecked")
	public TreeMap<Integer, ArrayList <TreeMap <String, Roi> > > getRoiList(){
		if(imageTrackList != null && !imageTrackList.isEmpty() && WindowManager.getCurrentImage() != null){
			TreeMap<Integer, ArrayList <TreeMap <String, Roi> > > roiList = new TreeMap<Integer, ArrayList <TreeMap <String, Roi> > >();
			/*for(ImageStateTracker tracker : imageTrackList){
				System.out.println("RoiList length: " + tracker.getRoiList().size());
				if(tracker.getRoiList().isEmpty()){
					TreeMap<String, Roi> pair = new TreeMap<String, Roi>();
					pair.put(tracker.getAngleOrientation(), (Roi) tracker.getDefaultRoi().clone());
					ArrayList <TreeMap <String, Roi>> list = new ArrayList <TreeMap <String, Roi>>();
					list.add(pair);
					roiList.put(tracker.getImageID(), list);
				}else{
					ArrayList <TreeMap <String, Roi>> list = new ArrayList <TreeMap <String, Roi>>();
					for(TreeMap<String, Roi> pair : tracker.getRoiList()){
						list.add((TreeMap<String, Roi>) pair.clone());
						roiList.put(tracker.getImageID(), list);
					}
				}
			}*/
			ImageStateTracker tracker = getTracker(WindowManager.getCurrentImage().getID());
			if(tracker != null && tracker.getRoiList().isEmpty()){
				TreeMap<String, Roi> pair = new TreeMap<String, Roi>();
				pair.put(tracker.getAngleOrientation(), (Roi) tracker.getDefaultRoi().clone());
				ArrayList <TreeMap <String, Roi>> list = new ArrayList <TreeMap <String, Roi>>();
				list.add(pair);
				roiList.put(tracker.getImageID(), list);
			}else if(tracker != null){
				ArrayList <TreeMap <String, Roi>> list = new ArrayList <TreeMap <String, Roi>>();
				for(TreeMap<String, Roi> pair : tracker.getRoiList()){
					list.add((TreeMap<String, Roi>) pair.clone());
					roiList.put(tracker.getImageID(), list);
				}
			}
			return roiList;
		}
		else return null;
		
	}
	public int getOperation(){
		return currentOperation;
	}
	public String getCurrentSelectionType(){
		return getTracker(WindowManager.getCurrentImage().getID()).getSelectionType();
	}
	public String getAngleOrientation(){
		return getTracker(WindowManager.getCurrentImage().getID()).getAngleOrientation();
	}
	public ImageStateTracker getTracker(int id){
		if(!imageTrackList.isEmpty()){
			for(ImageStateTracker tracker : imageTrackList){
				if(tracker.getImageID() == id)return tracker;
			}	
		}
		return null;
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		if(!(WindowManager.getImageCount() > 0))controller.getMainFrame().showInstructionsDialog("Please, first open an image!");
		//Choice listener
		else if(controller.calibrationIsInProgress() || controller.selectingStarIsInProgress()){/*do nothing*/}
		else if(e.getSource().getClass() == Choice.class){
			int currentImageID = WindowManager.getCurrentImage().getID();
			roiSelectionType = (String)e.getItem();
			selectionTypeChanged = true;
			currentOperation = Observable.DELETE;//instructs manageRoi to invoke delete operation + tells observer to clear overlay
			int operationExecuted = manageRoi(currentImageID);
			assert operationExecuted == RoiAdjustmentListener.ROI_DELETED_SELECTIONTYPE_CHANGED;
			assert getTracker(currentImageID).getImageID() == currentImageID;
			assert getTracker(currentImageID).getSelectionType() == roiSelectionType;
			assert getTracker(currentImageID).getAngleOrientation() == angleOrientation;
			assert getTracker(currentImageID).getRoiList().isEmpty();
			notifyObservers();
		}else{
			//Checkbox listener
			int currentImageID = WindowManager.getCurrentImage().getID();
			angleOrientation = ((Checkbox)e.getSource()).getLabel();
			angleChanged = true;
			int operationExecuted = manageRoi(currentImageID);
			assert getTracker(currentImageID).getImageID() == currentImageID;
			assert getTracker(currentImageID).getSelectionType() == roiSelectionType;
			assert getTracker(currentImageID).getAngleOrientation() == angleOrientation;
			if(roiSelectionType == RoiAdjustment.SINGLE){
				assert operationExecuted == RoiAdjustmentListener.DEFAULT_ROI_ANGLE_REWRITTEN || operationExecuted == RoiAdjustmentListener.ROI_DELETED_ANGLE_CHANGED || operationExecuted == RoiAdjustmentListener.RESET_ANGLE_FOR_MULTIPLE;
				assert getTracker(currentImageID).getRoiList().isEmpty();
				currentOperation = Observable.CLEAR;
				notifyObservers();
			}
		}	
	}
	@Override
	public void notifyObservers() {
		for(Observer o : observers){
			o.update(this);
		}	
	}
	@Override
	public void setObserver(Observer observer) {
		this.observers.add(observer);	
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		if(controller.calibrationIsInProgress() || controller.selectingStarIsInProgress()){/*do nothing*/}
		else if(e.getClickCount() == 2){
			if(WindowManager.getCurrentImage().getRoi() != null){
				currentOperation = Observable.PAINT;
				manageRoi(WindowManager.getCurrentImage().getID());
			}
			else currentOperation = Observable.NONE;
			notifyObservers();
		}		
	}
	@Override
	public void mouseEntered(MouseEvent arg0) {}
	@Override
	public void mouseExited(MouseEvent arg0) {}
	@Override
	public void mousePressed(MouseEvent arg0) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
//	LISTENERS  ******************************************************************
	@Override
	public void imageOpened(ImagePlus imp) {
		if(!(imp.getWindow() instanceof PlotWindow && !controller.selectingStarIsInProgress())){
			int imageID = imp.getID();
			
			imp.getCanvas().addMouseListener(this);
			imp.getCanvas().addKeyListener(this);
	
			//set default adjustments for newly opened image
			roiSelectionType = RoiAdjustment.SINGLE;
			angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
	
			WindowManager.getCurrentWindow().addWindowFocusListener(this);
////**************>>>>>>>>>		
			int operationExecuted = manageRoi(imageID);//adding initial Rois of just opened image to roiList
			assert operationExecuted == RoiAdjustmentListener.FIRST_DEFAULT_ROI_WRITTEN || operationExecuted == RoiAdjustmentListener.DEFAULT_ROI_WRITTEN;
			assert imageTrackList.size() > 0;
			assert getTracker(imageID).getImageID() == imageID;
			assert getTracker(imageID).getDefaultRoi().getBounds().getWidth() == WindowManager.getImage(imageID).getWidth();
			assert getTracker(imageID).getDefaultRoi().getBounds().getHeight() == WindowManager.getImage(imageID).getHeight();
			assert getTracker(imageID).getSelectionType() == RoiAdjustment.SINGLE;
			assert getTracker(imageID).getAngleOrientation() == RoiAdjustment.VERTICAL_ANGLE;
			assert getTracker(imageID).getRoiList().isEmpty();
			//assert imageTrackList.size() == WindowManager.getImageCount();
			
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
////**************>>>>>>>>>	
			
		}
		
	}
// end of LISTENERS	*************************************************************
	@Override
	public void imageClosed(ImagePlus imp) {
		closedImageID = imp.getID();
		if(!(imp.getWindow() instanceof PlotWindow))manageRoi(0);
	}
	@Override
	public void imageUpdated(ImagePlus imp) {}
	@Override
	public void keyPressed(KeyEvent e) {
		if(controller.calibrationIsInProgress() || controller.selectingStarIsInProgress()){/*do nothing*/}
		else{
			if(e.getKeyCode() == KeyEvent.VK_ENTER){
				if(WindowManager.getCurrentImage().getRoi() != null){
					int currentImageID = WindowManager.getCurrentImage().getID();
					currentOperation = Observable.PAINT;
					int operationExecuted = manageRoi(currentImageID);
					notifyObservers();
					assert getTracker(currentImageID).getImageID() == currentImageID;
					assert getTracker(currentImageID).getSelectionType() == roiSelectionType;
					assert getTracker(currentImageID).getAngleOrientation() == angleOrientation;
					assert getTracker(currentImageID).getRoiList().size() > 0;
				}
			}
			else if(e.getKeyCode() == KeyEvent.VK_DELETE){
				currentOperation = Observable.DELETE;
				int currentImageID = WindowManager.getCurrentImage().getID();
				int operationExecuted =  manageRoi(currentImageID);
				notifyObservers();
				currentOperation = Observable.RESET_GUI;
				notifyObservers();
				assert getTracker(currentImageID).getImageID() == currentImageID;
				assert getTracker(currentImageID).getSelectionType() == roiSelectionType;
				assert getTracker(currentImageID).getAngleOrientation() == angleOrientation;
				assert getTracker(currentImageID).getRoiList().isEmpty();
			}
		}
		
	}
	@Override
	public void keyReleased(KeyEvent arg0) {}
	@Override
	public void keyTyped(KeyEvent arg0) {}
	@Override
	public void windowGainedFocus(WindowEvent e) {
		if(controller.calibrationIsInProgress() || controller.selectingStarIsInProgress()){/*do nothing*/}
		else{
			int currentImageID = ((ImageWindow)e.getSource()).getImagePlus().getID();
			if(currentImageID != closedImageID && getTracker(currentImageID) != null){
				//Set current varificator state to latest state of selected image
				roiSelectionType = getTracker(currentImageID).getSelectionType();
				angleOrientation = getTracker(currentImageID).getAngleOrientation();
				//Set current GUI state to latest state of selected image
				currentOperation = Observable.MATCH_GUI;
				notifyObservers();	
			}	
		}
	}
	@Override
	public void windowLostFocus(WindowEvent arg0) {}
	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		if(WindowManager.getImageCount() == 0){
			readyForCalculation = false;
			controller.getMainFrame().showInstructionsDialog("There is no image opened!");
		}else if(WindowManager.getCurrentImage() == null){
			readyForCalculation = false;
			controller.getMainFrame().showInstructionsDialog("Please, select an Image, from which you would like to calculate MTF");
		}else if(controller.calibrationIsInProgress()){
			readyForCalculation = false;
			controller.getMainFrame().showInstructionsDialog("Calibration is in Progress!");
		}
		return readyForCalculation;
	}
	@Override
	protected void resetListener() {
		/*
		 * 
		angleChanged = false;
		roiSelectionType = RoiAdjustment.SINGLE;
		angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
		 */
	}	
}
class ImageStateTracker{
	private int id;
	private String roiSelectionType;
	private String angleOrientation;
	private ArrayList<TreeMap<String, Roi>> roiList;
	private Roi defaultRoi;
	
	ImageStateTracker(int imageID){
		this.id = imageID;
		this.roiSelectionType = RoiAdjustment.SINGLE;
		this.angleOrientation = RoiAdjustment.VERTICAL_ANGLE;
		this.defaultRoi = new Roi(0, 0, WindowManager.getImage(imageID).getWidth(), WindowManager.getImage(imageID).getHeight());
		roiList = new ArrayList<TreeMap<String, Roi>>();
	}
	public int getImageID(){
		return id;
	}
	public String getSelectionType(){
		return roiSelectionType;
	}
	public void setSelectionType(String selectionType){
		this.roiSelectionType = selectionType == RoiAdjustment.SINGLE ? selectionType : (selectionType == RoiAdjustment.MULTIPLE ? selectionType : this.roiSelectionType);
	}
	public String getAngleOrientation(){
		return angleOrientation;
	}
	public void setAngleOrientation(String angle){
		this.angleOrientation = angle == RoiAdjustment.VERTICAL_ANGLE ? angle : (angle == RoiAdjustment.HORISONTAL_ANGLE ? angle : this.angleOrientation);
	}
	public ArrayList<TreeMap<String, Roi>> getRoiList(){
		return roiList;
	}
	public void clearRoiList(){
		if(!roiList.isEmpty()){
			roiList.clear();
		}
	}
	void addAngleRoiPair(String angle, Roi roi, String selectionType){
		if(angle == RoiAdjustment.VERTICAL_ANGLE || angle == RoiAdjustment.HORISONTAL_ANGLE){
			TreeMap<String, Roi> map = new TreeMap<String, Roi>();
			map.put(angle, roi);
			roiList.add(map);
			setSelectionType(selectionType);
			setAngleOrientation(angle);
		}
	}
	public Roi getDefaultRoi(){
		return defaultRoi;
	}
}
class LPAdjustmentListener  extends MTFListener implements ItemListener, ActionListener, FocusListener{
	
	private Controller      controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	private String              lp = LPAdjustment.LP_PH;
	private String		        sizeMeasure = LPAdjustment.MM;
	private double				pixelSize =  1.0;
	private double				pixelSizeNormalized = 1.0;
	private double 				k=1;
	
	LPAdjustmentListener(Controller controller, Observer observer) {
		this.controller = controller;
		this.observers.add(observer);
	}
	
	public double getPixelSize(){
		return pixelSizeNormalized;
	}
	public boolean phIsSelected(){
		return lp == LPAdjustment.LP_PH;
	}
	@Override
	public void notifyObservers() {
		for(Observer observer : observers){
			observer.update(this);
		}	
	}
	@Override
	public void setObserver(Observer observer) {
		this.observers.add(observer);	
	}
	@Override
	public void itemStateChanged(ItemEvent e) {
		Choice select = (Choice)e.getSource();
		String eSourceName = select.getName();
		if(eSourceName == LPAdjustment.SELECT_LP){
			
			lp = select.getSelectedItem();
					//(String)e.getItemSelectable().getSelectedObjects()[0];
			if(lp != LPAdjustment.LP_MM){
				sizeMeasure = LPAdjustment.MM;
				pixelSize =  1.0;
				pixelSizeNormalized = 1.0;
				k=1;
			}
			notifyObservers();
		}else if (eSourceName == LPAdjustment.SELECT_SIZE){
			sizeMeasure = select.getSelectedItem();
			System.out.println("Size Measure : " + sizeMeasure);
			if(sizeMeasure == LPAdjustment.MKM){
				k=0.001;
			}else if(sizeMeasure == LPAdjustment.NM){
				k=0.000001;
			}else{
				k=1;
			}
			pixelSizeNormalized = pixelSize *k;
		}else{
			assert false : "Unknown ItemEvent source";
		}
	}
	public void focusLost(FocusEvent e) {
		((TextField)e.getSource()).transferFocus();
		String input = ((TextField)e.getSource()).getText();
		if(!input.isEmpty()){
			if(CustomUtils.stringParsableToDouble(input)){
				pixelSize = (double)Double.valueOf(input);
				pixelSizeNormalized = pixelSize*k;
			}else{
				currentOperation = Observable.DEFAULT;
				notifyObservers();
				controller.getMainFrame().showInstructionsDialog("Pixel Size must be a number!");
			}
		}else{
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
		}
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		((TextField)e.getSource()).transferFocus();
		String input = ((TextField)e.getSource()).getText();
		if(!input.isEmpty()){
			if(CustomUtils.stringParsableToDouble(input)){
				pixelSize = (double)Double.valueOf(input);
				pixelSizeNormalized = pixelSize*k;
			}else{
				currentOperation = Observable.DEFAULT;
				notifyObservers();
				controller.getMainFrame().showInstructionsDialog("Pixel Size must be a number!");
			}
		}else{
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
		}	
	}
	@Override
	public int getOperation() {
		return currentOperation;
	}

	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		return readyForCalculation;
	}

	@Override
	protected void resetListener() {
		lp = LPAdjustment.LP_PH;
		sizeMeasure = LPAdjustment.MM;
		pixelSize =  1.0;
		pixelSizeNormalized = 1.0;
		currentOperation = Observable.NONE;
		k=1;
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}
	
}
class DeltaSAdjustmentListener  extends MTFListener implements ActionListener, FocusListener{
	
	private Controller      controller;
	private ArrayList<Observer> observers = new ArrayList<Observer>();
	private double              quotient = 1.0;
	
	DeltaSAdjustmentListener(Controller controller, Observer observer) {
		this.controller = controller;
		this.observers.add(observer);
	}

	public double getQuoitient(){
		return this.quotient;
	}
	@Override
	public void notifyObservers() {
		for(Observer observer : observers){
			observer.update(this);
		}
	}
	@Override
	public void setObserver(Observer observer) {
		this.observers.add(observer);
	}
	@Override
	public int getOperation() {
		return currentOperation;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		((TextField)e.getSource()).transferFocus();
		String input = ((TextField)e.getSource()).getText();
		if(!input.isEmpty()){
			if(CustomUtils.stringParsableToDouble(input)){
		
				quotient = Double.valueOf(input);
				if(quotient < 1.0 || quotient > 1000){
					currentOperation = Observable.RESET_GUI;
					notifyObservers();
					if(quotient < 1.0)controller.getMainFrame().showInstructionsDialog("Delta S quotient must be bigger than 0!");
					else controller.getMainFrame().showInstructionsDialog("Delta S quotient must be less than or equal to 1000!");
					quotient = 1.0;
				}

			}else{
				currentOperation = Observable.RESET_GUI;
				notifyObservers();
				controller.getMainFrame().showInstructionsDialog("Delta S quotient must be a number between 1 and 1000 inclusive!");
			}
		}else{
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
		}	
	}

	@Override
	public boolean varify() {
		boolean readyForCalculation = true;
		return readyForCalculation;
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void focusLost(FocusEvent e) {
		((TextField)e.getSource()).transferFocus();
		String input = ((TextField)e.getSource()).getText();
		if(!input.isEmpty()){
			try{
				quotient = (double)Integer.valueOf(input);
				if(quotient < 0.0 || quotient > 1.0){
					currentOperation = Observable.RESET_GUI;
					notifyObservers();
					if(quotient < 0.0)controller.getMainFrame().showInstructionsDialog("Delta S quotient must be bigger than 0.0!");
					else controller.getMainFrame().showInstructionsDialog("Delta S quotient must be less than or equal to 1.0!");
					quotient = 1.0;
				}
			}catch(NumberFormatException ex1){
				try{
					quotient = Double.valueOf(input);
					if(quotient < 0.0 || quotient > 1.0){
						currentOperation = Observable.RESET_GUI;
						notifyObservers();
						if(quotient < 0.0)controller.getMainFrame().showInstructionsDialog("Delta S quotient must be bigger than 0.0!");
						else controller.getMainFrame().showInstructionsDialog("Delta S quotient must be less than or equal to 1.0!");
						quotient = 1.0;
					}
				}catch(NumberFormatException ex2){
					currentOperation = Observable.RESET_GUI;
					notifyObservers();
					controller.getMainFrame().showInstructionsDialog("Delta S quotient must be a number between 0.0 and 1.0 exclusive!");
				}
			}
		}else{
			currentOperation = Observable.RESET_GUI;
			notifyObservers();
		}	
		
	}

	@Override
	protected void resetListener() {
		quotient = 1.0;	
	}
}
class LSFAdjustmentListener  extends MTFListener{
	private MTFController controller;
	private ArrayList<Observer> observers;
	LSFAdjustmentListener(MTFController controller, Observer observer) {
		this.controller = controller;
		this.observers.add(observer);
	}

	@Override
	public void notifyObservers() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setObserver(Observer observer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getOperation() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean varify() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected void resetListener() {
		// TODO Auto-generated method stub
		
	}	
}


////////////////////////////////////////////////////////////////
//                                                            //
//                       MODEL                                //
//                                                            //
////////////////////////////////////////////////////////////////
abstract class Calculator{
	Controller controller;
	
	Calculator(Controller controller){
		this.controller = controller;
	}
	public abstract void calculate();
	protected void reset(){
		System.out.println("In calculator reset");
		controller.resetGUI();
		controller.resetListeners();
		
	}
	
}

class MTFCalculator extends Calculator{
	
	MTFController                                             mtfController;//RoiData
	private TreeMap<Integer, ArrayList<TreeMap<String, Roi>>> roiList;
	private boolean                                           phIsSet;       //LPData
	private double                                            pixelSize;
	private double                                            quotient;     //DeltaSData
	private int												  imageHeight;
	
	//private double                             deltaS;
	//private CurveFitter                        lineFitter;//for drawing an angle
	//boolean                                    angleIsFinded = false;
	private ImagePlus                          currentWorkImage = new ImagePlus();
	//private double[]                           pixelsValues;
	//private TreeMap<Double, Double> 		   distanceValueMap = new TreeMap<Double, Double>(); //Distance to line and Pixel Intensity
	//private TreeMap<Double, Double>			   averageMap = new TreeMap<Double, Double>(); // Average value of distanceValueMap
	//private TreeMap<Double, Double> 		   difrMap = new TreeMap<Double, Double>(); // Differentiation of averageMap
	//private TreeMap<Double, Double> 		   intensityMTFMap;
	private ArrayList<TreeMap<Double, Double>> mtfList = new ArrayList<TreeMap<Double, Double>>();
	private TreeMap<Double, Double>            resultantMTFMap = new TreeMap<Double, Double>();
	//private ImagePlus currentRegion;
	
	private MTFCalculator(Builder builder){
		super(builder.mtfController);
		this.mtfController = builder.mtfController;
		resetParameters(builder);	
	}
	private void resetParameters(Builder builder){
		this.imageHeight = builder.imageHeight;
		this.roiList = builder.roiList;
		this.quotient = builder.quotient;
		this.phIsSet = builder.phIsSet;
		this.pixelSize = builder.pixelSize;
		System.out.println("Pixel Size: " + this.pixelSize);
	}
	//////////////////// MTF BUILDER ///////////////////////////
	public static class Builder{
		private MTFCalculator calc;
		//required parameters
		private final MTFController 							  mtfController;
		
		private TreeMap<Integer, ArrayList<TreeMap<String, Roi>>> roiList;
		private boolean                                           phIsSet;       //LPData
		private double                                            pixelSize;
		private double                                            quotient;     //DeltaSData
		private int 											  imageHeight;
		
		public Builder(MTFController mtfController){
			this.mtfController = mtfController;
		}
		public void imageHeight(int imageHeight) {
			this.imageHeight = imageHeight;
			
		}
		public Builder roiList(TreeMap<Integer, ArrayList<TreeMap<String, Roi>>> roiList){
			this.roiList = roiList;
			return this;
		}
		public Builder quotient(double quotient){
			this.quotient = quotient;
			return this;
		}
		public Builder PH(boolean phIsSet){
			this.phIsSet = phIsSet;
			return this;
		}
		public Builder pixelSize(double pixelSize){
			this.pixelSize = pixelSize;
			return this;
		}		
		public MTFCalculator build(){
			if(calc == null)calc = new MTFCalculator(this);
			else calc.resetParameters(this);
			return calc;
		}
	}
/////////////////////////////////////////////////////////////////
	public void calculate(){
		gatherMTFList();
		//reset();
	}
	private void gatherMTFList(){
		if(roiList != null && !roiList.isEmpty()){
			Integer key = roiList.firstKey();
			checkColorMode(key);
			ArrayList<TreeMap<String, Roi>> list = roiList.get(key);
			//System.out.println("RoiList length: " + list.size());
			Overlay overlay;
			if(WindowManager.getImage(key).getOverlay() == null){
				overlay = new Overlay();
			}
			else overlay = WindowManager.getImage(key).getOverlay();
			overlay.clear();
			mtfList.clear();
			for(TreeMap<String, Roi> pair : list){
				//prepare current region
				String angle = pair.firstKey(); Roi roi = pair.get(angle);
				ImagePlus currentRegion = new ImagePlus("CurrentRegion");
				if(roi.getBounds().getWidth() == currentWorkImage.getWidth() && roi.getBounds().getHeight() == currentWorkImage.getHeight()){
					currentRegion = (ImagePlus)(currentWorkImage.clone());
				}else{
					ImageProcessor proc = (ImageProcessor)(currentWorkImage.getProcessor().clone());
					proc.setRoi((Roi)roi.clone());
					currentRegion.setProcessor(proc.crop());
				}
				
				ImageProcessor proc = currentRegion.getProcessor();
				double[] bwValues;
				double[] pixelsValues;
				//convert to Black&White
				if(proc.getBitDepth() == 16){
					short[] sourcePixels = (short[])proc.getPixels();
					pixelsValues = new double[sourcePixels.length];
					for(int i=0;i<sourcePixels.length;i++){
						pixelsValues[i]= sourcePixels[i] & 0xffff;
					}
					bwValues = new double[sourcePixels.length];
					double middleVal = proc.getAutoThreshold();;
					for(int i=0;i<sourcePixels.length;i++){
						if(pixelsValues[i] <= middleVal){
							bwValues[i]=0.0;
						}else{
							bwValues[i]=65635.0;
						}
					}
				}else{
					byte[] sourcePixels = (byte[])proc.getPixels();
					pixelsValues = new double[sourcePixels.length];
					for(int i=0;i<sourcePixels.length;i++){
						pixelsValues[i]= sourcePixels[i] & 0xff;
					}
					bwValues = new double[sourcePixels.length];
					double middleVal = proc.getAutoThreshold();		
					//middleVal = middleVal+middleVal*0.15;
					System.out.println("Middle val: " + middleVal);
					for(int i=0;i<sourcePixels.length;i++){
						if(pixelsValues[i] <= middleVal){
							bwValues[i]=0.0;
						}else{
							bwValues[i]=255.0;
						}
					}
				}
				
				ArrayList<Double> xVal = new ArrayList<Double>();
				ArrayList<Double> yVal = new ArrayList<Double>();
				//differentiation
				if(angle == RoiAdjustment.VERTICAL_ANGLE){
					for(int i=0;i<proc.getHeight();i++){
						double max = 0.0;
						double x = 0.0;
						for(int j=1;j<proc.getWidth();j++){
							double diff = Math.abs(bwValues[j+i*proc.getWidth()]-bwValues[(j+i*proc.getWidth())-1]);
							if(diff>max){
								max = diff;
								x = j;
							}
						}
						if(max != 0.){
							xVal.add((double)x);
							yVal.add((double)i);
						}
					}
				}else{
					for(int i=0;i<proc.getWidth();i++){
						double max = 0.0;
						double y = 0.0;
						for(int j=1;j<proc.getHeight();j++){
							double diff = Math.abs(bwValues[i+j*proc.getWidth()]-bwValues[(i+(j-1)*proc.getWidth())]);
							if(diff>max){
								max = diff;
								y = j;
							}
						}
						if(max != 0.){
							xVal.add((double)i);
							yVal.add((double)y);
						}
					}
				}
				double[] xArr = new double[xVal.size()];
				double[] yArr = new double[yVal.size()];
				for(int i=0;i<xVal.size();i++){
					xArr[i]=xVal.get(i);
					yArr[i]=yVal.get(i);	
				}
				// find angle and line
				CurveFitter lineFitter = new CurveFitter(xArr, yArr);
				lineFitter.doFit(CurveFitter.STRAIGHT_LINE);
				
				//draw Angle
				Roi rect = new Roi (roi.getXBase(), roi.getYBase(), currentRegion.getWidth(), currentRegion.getHeight());
				Roi line;
				if(angle == RoiAdjustment.VERTICAL_ANGLE)line = new Line((1-lineFitter.getParams()[0])/lineFitter.getParams()[1] + roi.getXBase(), 1 + roi.getYBase(), (proc.getHeight()-1-lineFitter.getParams()[0])/lineFitter.getParams()[1] + roi.getXBase(), proc.getHeight()-1 + roi.getYBase());
				else line = new Line(1 + roi.getXBase(), lineFitter.getParams()[0]+lineFitter.getParams()[1] + roi.getYBase(), proc.getWidth()-1 + roi.getXBase(), lineFitter.getParams()[0] + lineFitter.getParams()[1]*(proc.getWidth()-1) + roi.getYBase() );
				if(angle == RoiAdjustment.VERTICAL_ANGLE){
					rect.setStrokeColor(Color.RED);
					line.setStrokeColor(Color.RED);
				}else {
					rect.setStrokeColor(Color.GREEN);
					line.setStrokeColor(Color.GREEN);
				}
				overlay.add(rect);
				overlay.add(line);
				
				proc = (ImageProcessor)(currentRegion.getProcessor().clone());
				double angleGrad =  Math.toDegrees(Math.atan(1.0/(lineFitter.getParams())[1]));
				//double angleGrad1 = -2.338;
				//System.out.println(lineFitter.getResultString());
				//System.out.println(angel);
				
				double[] distanceToLine = new double[pixelsValues.length]; // opredelyem rastoyanie do linii
				double roiAngle =  angleGrad*Math.PI/180;
				System.out.println("Angle: "+angleGrad);
				for(int i=0;i<proc.getHeight();i++){
					for(int j=0;j<proc.getWidth();j++){
						//distance to line for every pixel;
						distanceToLine[j+i*proc.getWidth()] = pixelSize*1*(j*Math.cos(roiAngle)-i*Math.sin(roiAngle));
						//if(j+i*proc.getWidth() < 400)System.out.println("x=" + j+" y="+i+"   " +distanceToLine[j+i*proc.getWidth()]);
					}
				}
						
				TreeMap<Double, Double> distanceValueMap = new TreeMap<Double, Double>();
				for(int i=0;i<pixelsValues.length;i++){
					distanceValueMap.put(distanceToLine[i], (double)pixelsValues[i]);
					//if(i<200)System.out.println("KEY:"+proection[i]+"   "+"VALUE:"+distanceValueBeinding.get(proection[i]));
				}
				System.out.println("makeProjection(ImagePlus currentRegion)");
					
				double deltaS = quotient*pixelSize; // коэфф и размер пикмеля
				int k = 0;
				if(distanceValueMap.firstKey()<0){
					if(distanceValueMap.firstKey()<-deltaS/2){
						k = -((int)((Math.abs(distanceValueMap.firstKey())-deltaS/2)/deltaS) + 1);
						System.out.println("k<0 : "+k);
					}
				}
				Set<Double> DVkeys = distanceValueMap.keySet();
				//System.out.println(distanceValueMap);
				TreeMap<Double, Double> averageMap = new TreeMap<Double, Double>();
				double avg = 0;
				int count=0;
				for(Double distance : DVkeys){
					//System.out.println(key);
					//do{
					if(Math.abs(distance-k*deltaS)<=deltaS/2){
						avg=avg+distanceValueMap.get(distance);
						//System.out.println("1 if");
						count++;
					}else{
						if(count>0){
							avg=avg/count;
							averageMap.put((double)k, avg);
							k++;
							avg=0;
							count=0;
						}else{
							count=0;
							k++;
						      // System.out.println(k);
						}
						//System.out.println("else");
					}	
							//}while(avg == 0 /*&& distanceValueMap.get(distance) != 0.*/);
				}
				System.out.println("calcAverageSpread()");
			// calculate LSF
				Set<Double> AMkeys = averageMap.keySet();
				TreeMap<Double, Double> difrMap = new TreeMap<Double, Double>();
				double previousVal = 0.0;
				Iterator<Double> AMKeys = AMkeys.iterator();
				if(AMKeys.hasNext()){previousVal = averageMap.get(AMKeys.next());}
				while ( AMKeys.hasNext()) {
				     double AMkey = (double) AMKeys.next();
				    /*if(previousVal == 0) {
				    	 previousVal = averageMap.get(AMkey);
				    	 difrMap.put(AMkey, 0.0);
				     }else{*/
				    	 if(AMKeys.hasNext()){
				    		 difrMap.put(AMkey, (averageMap.get(averageMap.higherKey(AMkey)) - previousVal)/2);//*deltaS);
				    		 previousVal = averageMap.get(AMkey);
				    	 }else{
				    		 difrMap.put(AMkey, 0.0);
				    	 }
				    // }
				}
				System.out.println("calcLineSpread()");
				
				double n = difrMap.size();
				double h = deltaS;
				double T = n*h;
				double DFT[] = new double[(int)n/2];
				for(int j = 0; j < (int)n/2; j++){
					Set<Double> DMkeys = difrMap.keySet();
					double fs = 0.0;
					double ss = 0.0;
					k = 0;
					for(Double DMkey : DMkeys){
						//if(key >= 48. && key <= 75.){
						fs = fs + difrMap.get(DMkey)*Math.cos(2*Math.PI*j*k/n);
						ss = ss + difrMap.get(DMkey)*Math.sin(2*Math.PI*j*k/n);
						k++;
						//}
					}
					DFT[j] = Math.abs(Math.sqrt(Math.pow(fs, 2.0)+Math.pow(ss, 2.0)));
				}
				double normalizer = 1/DFT[0];
				//double [] normalizedDFT = new double [DFT.length];
				TreeMap<Double, Double> intensityMTFMap = new TreeMap<Double, Double>();
				int height = 1;
				if(phIsSet)height = imageHeight;
				 
				for(int i = 0; i<DFT.length*quotient; i++ ){
					double IMkey = i*height/(2*quotient*DFT.length*pixelSize);
					double val = DFT[i]*normalizer;
					intensityMTFMap.put(IMkey, val);
				}
				
				System.out.println("calcMTF()");
				if(list.size() > 1){
					mtfList.add(intensityMTFMap);
				}else{
					CustomUtils.drawPlot(averageMap, "Edge Spread Function", "pixels", "intensity");
					CustomUtils.drawPlot(difrMap, "Line Spread Function", "pixels", "values");
					CustomUtils.drawPlot(intensityMTFMap, "Modulation Transfer Function (normalized)", "frequency", "intensity");
				}
			}
			for(TreeMap<Double, Double>intensityMTFMap: mtfList){
				CustomUtils.drawPlot(intensityMTFMap, "Modulation Transfer Function (normalized)", "frequency", "intensity");
			}
			if(list.size() > 1)constructResultantMTF();
			WindowManager.getImage(key).setOverlay(overlay);
			CustomUtils.resetRoiColor();
			roiList.remove(key);
			//gatherMTFList();
			
		}
	}
	private void constructResultantMTF(){

	/*Test Map
	 * 
	 * 	TreeMap<Double, Double> fm = new TreeMap<Double, Double>();
		fm.put(0., 1.);
		fm.put(10., 0.96);
		fm.put(25., 0.85);
		fm.put(45., 0.75);
		fm.put(60., 0.15);
		fm.put(95., 0.08);
		fm.put(135., 0.05);
		TreeMap<Double, Double> sm = new TreeMap<Double, Double>(); 
		sm.put(0., 1.);
		sm.put(15., 0.95);
		sm.put(25., 0.75);
		sm.put(35., 0.6);
		sm.put(55., 0.55);
		sm.put(75., 0.48);
		sm.put(95., 0.3);
		sm.put(105., 0.2);
		sm.put(120., 0.15);
		mtfList.add(fm);
		mtfList.add(sm); */
		
		for(TreeMap<Double, Double> mtfA: mtfList){		
			
			for(Double keyA : mtfA.keySet()){
				Double sumVal = mtfA.get(keyA);
				int count = 1;
				if(!resultantMTFMap.isEmpty() && !resultantMTFMap.containsKey(keyA)){
					for(TreeMap<Double, Double> mtfB: mtfList){
						if(mtfA != mtfB){	
							if(mtfB.floorEntry(keyA) != null){//resultantMTFMap.put(keyA, mtfB.ceilingEntry(keyA).getValue());
								sumVal = sumVal + mtfB.floorEntry(keyA).getValue();
								count++;	
							}
						}
					}
				}
				resultantMTFMap.putIfAbsent(keyA, sumVal/count);
			}
		}
		CustomUtils.drawPlot(resultantMTFMap, "Modulation Transfer Function (normalized)", "frequency", "intensity");
		resultantMTFMap.clear();
	}
	private void checkColorMode(int imageID){
		ImagePlus img = WindowManager.getImage(imageID);
		int bitDepth = img.getBitDepth();
		//if(bitDepth == ImagePlus.COLOR_256 || bitDepth == ImagePlus.COLOR_RGB || bitDepth == ImagePlus.GRAY32){
		System.out.println(bitDepth);
		//System.out.println(ImagePlus.GRAY16);
		if(bitDepth == 8 || bitDepth == 16){
			currentWorkImage.setProcessor(((ImageProcessor)(img.getProcessor().clone())));
		}else{
			currentWorkImage.setProcessor(((ImageProcessor)(img.getProcessor().clone())).convertToByteProcessor());
		}
		bitDepth = currentWorkImage.getProcessor().getBitDepth();
		assert bitDepth == 8 || bitDepth == 16;
	}

}
class StarMTFCalculator extends Calculator{
	private StarMTFController starMTFController;
	
	private ImagePlus normalizedStar;

	private TreeMap<Double, TreeMap<Double, Double>> starDataMap = new TreeMap<Double, TreeMap<Double, Double>>();
	private TreeMap<Integer, TreeMap<Double, ArrayList<Double>>> sectorRadiusContrastMap = new TreeMap<Integer, TreeMap<Double, ArrayList<Double>>>();
	private TreeMap<Integer, TreeMap<Double, Double>> sectorRadiusExMap = new TreeMap<Integer, TreeMap<Double, Double>>();
	private TreeMap<Integer, TreeMap<Double, Double>> sectorFourierSeriesMap = new TreeMap<Integer, TreeMap<Double, Double>>();
	//params
	private String starType;
	private String sectorsNum;
	private int cyclesNum;
	private int imageHeight;
	private ImageProcessor proc;
	private int centerX, centerY;
	
	StarMTFCalculator(Builder builder) {
		super(builder.starMTFController);
		this.starMTFController = builder.starMTFController;
		normalizedStar = builder.starImage;//WindowManager.getCurrentImage();//
		CustomUtils.checkColorMode(normalizedStar);
		proc = normalizedStar.getProcessor();
		centerX = centerY = normalizedStar.getWidth()/2;
		CustomUtils.checkColorMode(normalizedStar);
		resetParameters(builder);
	}
	private void resetParameters(Builder builder){
		this.imageHeight = builder.imageHeight;
		this.starType = builder.starType;
		this.sectorsNum = builder.sectorsNum;
		this.cyclesNum = builder.cyclesNum;
	}
////////////////////MTF BUILDER ///////////////////////////
	public static class Builder{
		private StarMTFCalculator calc;
		//required parameters
		private final StarMTFController starMTFController;
		private int imageHeight;
		private String starType;
		private String sectorsNum;
		private int cyclesNum;
		private ImagePlus starImage;
		//
		
		public Builder(StarMTFController starMTFController){
			this.starMTFController = starMTFController;
		}
		public void starImage(ImagePlus starImage) {
			this.starImage = starImage;
			
		}
		public void sectorsNum(String sectorsNum) {
			this.sectorsNum = sectorsNum;
			System.out.println("Sectors Num in builder " + sectorsNum);
			
		}
		public void cyclesNum(int cyclesNum) {
			this.cyclesNum = cyclesNum;
			System.out.println("Cycles Num in builder " + cyclesNum);
			
		}
		public void starType(String starType) {
			this.starType = starType;
			
		}
		public Builder imageHeight(int imageHeight){
			this.imageHeight = imageHeight;
			return this;
		}
		
		public StarMTFCalculator build(){
			if(calc == null)calc = new StarMTFCalculator(this);
			else calc.resetParameters(this);
			return calc;
		}
	}
/////////////////////////////////////////////////////////////////
	

	@Override
	public void calculate() {
		gatherStarDataMap();
		gatherSectorRadiusContrastMap();
		gatherSectorRadiusExMap();
		if(starType == StarTypeAdjustment.BINARY)gatherSectorFourierSeriesMap();
		controller.resetParticleListeners();
		displayResults();
		//
		//reset();
		
	}
	private void displayResults(){
		CustomUtils.drawPlot(starDataMap.get(starDataMap.lastKey()), "Selected Outer Radius Data" , "Angle", "Value");
		
		System.out.println("SectorRadiusContrastMap : " + sectorRadiusContrastMap);
		Plot contrastPlot = CustomUtils.drawContrastPlot( sectorRadiusContrastMap.get(1), imageHeight, "MTF Sectors", "frequency", "contrast");
		if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
			CustomUtils.addCurveToContrastPlot(contrastPlot, new Color(SectorsAdjustment.SECTOR_7_COLOR), sectorRadiusContrastMap.get(7));
			CustomUtils.addCurveToContrastPlot(contrastPlot, new Color(SectorsAdjustment.SECTOR_2_COLOR), sectorRadiusContrastMap.get(2));
			CustomUtils.addCurveToContrastPlot(contrastPlot, new Color(SectorsAdjustment.SECTOR_3_COLOR), sectorRadiusContrastMap.get(3));
			CustomUtils.addCurveToContrastPlot(contrastPlot, new Color(SectorsAdjustment.SECTOR_4_COLOR), sectorRadiusContrastMap.get(4));
			CustomUtils.addCurveToContrastPlot(contrastPlot, new Color(SectorsAdjustment.SECTOR_5_COLOR), sectorRadiusContrastMap.get(5));
		}
			//CustomUtils.addCurveToContrastPlot(contrastPlot, Color.RED, sectorRadiusContrastMap.get(6));
		
		//addCurveToPlot2(plot, Color.PINK, sectorRadiusContrastMap.get(8), false);
		
		Plot exPlot = CustomUtils.drawPlot( sectorRadiusExMap.get(1), "Excess Kurtosis (Woelbung)", "frequency", "Difference");
		if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
			CustomUtils.addCurveToPlot(exPlot, new Color(SectorsAdjustment.SECTOR_7_COLOR), sectorRadiusExMap.get(7));
			CustomUtils.addCurveToPlot(exPlot, new Color(SectorsAdjustment.SECTOR_2_COLOR), sectorRadiusExMap.get(2));
			CustomUtils.addCurveToPlot(exPlot, new Color(SectorsAdjustment.SECTOR_3_COLOR), sectorRadiusExMap.get(3));
			CustomUtils.addCurveToPlot(exPlot, new Color(SectorsAdjustment.SECTOR_4_COLOR), sectorRadiusExMap.get(4));
			CustomUtils.addCurveToPlot(exPlot, new Color(SectorsAdjustment.SECTOR_5_COLOR), sectorRadiusExMap.get(5));
		}
		//CustomUtils.addCurveToPlot(plot1, Color.RED, sectorRadiusExMap.get(6), false);
		//CustomUtils.addCurveToPlot(plot1, Color.PINK, sectorRadiusExMap.get(8), false);
		/*Plot plot2 = drawPlot2( sectorRadiusDispMap.get(1), "Standard Deviation", "frequency", "Difference", true);
		addCurveToPlot(plot2, Color.YELLOW, sectorRadiusDispMap.get(7), false);
		addCurveToPlot(plot2, Color.GREEN, sectorRadiusDispMap.get(2), false);
		addCurveToPlot(plot2, Color.BLUE, sectorRadiusDispMap.get(3), false);
		addCurveToPlot(plot2, Color.MAGENTA, sectorRadiusDispMap.get(4), false);
		addCurveToPlot(plot2, Color.ORANGE, sectorRadiusDispMap.get(5), false);*/
		if(starType == StarTypeAdjustment.BINARY){
			Plot fourierSeriesPlot = CustomUtils.drawPlot( sectorFourierSeriesMap.get(1), "Fourier Series", "frequency", "Difference");
			if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
				CustomUtils.addCurveToPlot(fourierSeriesPlot, new Color(SectorsAdjustment.SECTOR_7_COLOR), sectorFourierSeriesMap.get(7));
				CustomUtils.addCurveToPlot(fourierSeriesPlot, new Color(SectorsAdjustment.SECTOR_2_COLOR), sectorFourierSeriesMap.get(2));
				CustomUtils.addCurveToPlot(fourierSeriesPlot, new Color(SectorsAdjustment.SECTOR_3_COLOR), sectorFourierSeriesMap.get(3));
				CustomUtils.addCurveToPlot(fourierSeriesPlot, new Color(SectorsAdjustment.SECTOR_4_COLOR), sectorFourierSeriesMap.get(4));
				CustomUtils.addCurveToPlot(fourierSeriesPlot, new Color(SectorsAdjustment.SECTOR_5_COLOR), sectorFourierSeriesMap.get(5));
			}
		}
	}
	private void gatherStarDataMap(){
		System.out.println("proc : "+ proc);
		/*for(int i = 0; i < proc.getPixelCount(); i++){
			System.out.print(proc.get(i) + ", ");
		}*/
	
		
		for(int y = 1; y<centerY-10; y= y+5){
			double rad = centerY - y;
			double angle = 0;
			int currentX = centerX;
			int currentY = y;
			TreeMap<Double, Double> angleValueMap = new TreeMap<Double, Double>();
			starDataMap.put(rad, angleValueMap);
			while(angle < 2*Math.PI){
				double value = proc.getPixel(currentX, currentY);
				angleValueMap.put(angle, value);
				if(angle < Math.PI/2){
					double downPointRad = Math.sqrt( Math.pow(centerY - (currentY+1), 2) + Math.pow(currentX - centerX, 2));
					double rightPointRad = Math.sqrt( Math.pow(centerY - currentY, 2) + Math.pow(currentX+1 - centerX, 2));
					if(Math.abs(rad-downPointRad) < Math.abs(rad-rightPointRad))currentY++;
					else currentX++;
					double deltaY = centerY - currentY;
					double deltaX = currentX - centerX;
					angle = Math.atan(deltaX/deltaY);
				}else if(angle < Math.PI){
					double downPointRad = Math.sqrt( Math.pow(centerY - (currentY+1), 2) + Math.pow(currentX - centerX, 2));
					double leftPointRad =Math.sqrt( Math.pow(centerY - currentY, 2) + Math.pow(currentX-1 - centerX, 2));
					if(Math.abs(rad-downPointRad) < Math.abs(rad-leftPointRad))currentY++;
					else currentX--;
					if(currentX == centerX){
						angle = Math.PI;
					}else {
						double deltaY = centerY - currentY;
						double deltaX = currentX - centerX;
						angle = Math.PI/2 - Math.atan(deltaY/deltaX);
					}
				}else if(angle < 3*Math.PI/2){
					double upPointRad = Math.sqrt( Math.pow((currentY-1) - centerY, 2) + Math.pow(centerX - currentX, 2));
					double leftPointRad =Math.sqrt( Math.pow(currentY - centerY, 2) + Math.pow(centerX - (currentX-1), 2));
					if(Math.abs(rad-upPointRad) < Math.abs(rad-leftPointRad))currentY--;
					else currentX--;
					double deltaY = centerY - currentY;
					double deltaX = currentX - centerX;
					angle = Math.PI + (Math.PI/2 - Math.atan(deltaY/deltaX));
				}else{
					double upPointRad = Math.sqrt( Math.pow(centerY - (currentY-1), 2) + Math.pow(centerX - currentX, 2));
					double rightPointRad = Math.sqrt( Math.pow(centerY - currentY, 2) + Math.pow(centerX - (currentX+1), 2));
					if(Math.abs(rad-upPointRad) < Math.abs(rad-rightPointRad))currentY--;
					else currentX++;
					double deltaY = centerY - currentY;
					double deltaX = centerX - currentX;
					angle = 3*Math.PI/2 + Math.atan(deltaY/deltaX);
				}
			}
		}
		
		//System.out.println("KeySet : " + starDataMap.keySet());
		//CustomUtils.drawPlot(starDataMap.get(starDataMap.lastKey()-5), "Selected Outer Radius Data -5" , "Angle", "Value");
		//CustomUtils.drawPlot(starDataMap.get(starDataMap.lastKey() - 20), "Selected Outer Radius Data -20" , "Angle", "Value");
		
		
	}
	private void gatherSectorRadiusContrastMap(){
		int N = cyclesNum;   ////   количество  CYCLES
		System.out.println("N " + N);
		if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
			 ///   MTF SEKTORS разный для бинарной и гармонической звезды
			Set<Double> radiusSet = starDataMap.keySet();
			for(Double radius : radiusSet){
				TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
				Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
				int sector = 0;
				int G = 1;      // nomer garmoniki
				double b1 = 0.0;   // perscha garmonika
				double b2 = 0.0;   // perscha garmonika
				double sum = 0.0;  // seredne znachenya
				double b1LastSector = 0;
				double b2LastSector = 0;
				double sumLastSector = 0;
				double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;
				while(angleIterator.hasNext()){	
	                
					while(angle >= 13*Math.PI/8 && angle < (15*Math.PI/8) && angleIterator.hasNext()){	
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 7;
					}
					while(angle >= 11*Math.PI/8 && angle < (13*Math.PI/8) && angleIterator.hasNext()){
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 6;
					}
					while(angle >= 9*Math.PI/8 && angle < (11*Math.PI/8) && angleIterator.hasNext()){	
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 5;
					}
					while(angle >= 7*Math.PI/8 && angle < (9*Math.PI/8) && angleIterator.hasNext()){	
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 4;
					}
					while(angle >= 5*Math.PI/8 && angle < (7*Math.PI/8) && angleIterator.hasNext()){	
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 3;
					}
					while(angle >= 3*Math.PI/8 && angle < (5*Math.PI/8) && angleIterator.hasNext()){	
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 2;
					}
					while(angle >= Math.PI/8 && angle < (3*Math.PI/8) && angleIterator.hasNext()){
						b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
						b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
						sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 1;
					}
					while(angleIterator.hasNext() && angle >= 15*Math.PI/8 && angle < 2*Math.PI || angle >= 0 && angle < Math.PI/8){	
							b1LastSector = b1LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
							b2LastSector = b2LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
							sumLastSector = sumLastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 8;
					}
					if(!angleIterator.hasNext()){
						b1 = b1*8/Math.PI;			
						b2 = b2*8/Math.PI;
						sum  = sum*4/Math.PI;
					    double b = Math.sqrt(b1*b1 + b2*b2);
					    double contrast = starType == StarTypeAdjustment.HARMONY ? (b/sum) :  (b/sum)*Math.PI/4;      //for binary
					     if(!(sectorRadiusContrastMap.containsKey(sector))){
					    	sectorRadiusContrastMap.put(7, new TreeMap<Double, ArrayList<Double>>());
					     }
					    double rad = N*imageHeight/(2*Math.PI*radius);
					    sectorRadiusContrastMap.get(7).put(rad, new ArrayList<Double>());
					    sectorRadiusContrastMap.get(7).get(rad).add(contrast);
					    sectorRadiusContrastMap.get(7).get(rad).add(sum);
						
						b1LastSector = b1LastSector*8/Math.PI;			
						b2LastSector = b2LastSector*8/Math.PI;
						sumLastSector  = sumLastSector*4/Math.PI;
					    b = Math.sqrt(b1LastSector*b1LastSector + b2LastSector*b2LastSector);
					    contrast = starType == StarTypeAdjustment.HARMONY ? (b/sumLastSector) : (b/sumLastSector)*Math.PI/4; // for binary
					    if(!(sectorRadiusContrastMap.containsKey(sector))){
					    	sectorRadiusContrastMap.put(sector, new TreeMap<Double, ArrayList<Double>>());
					    }
					    sectorRadiusContrastMap.get(sector).put(rad, new ArrayList<Double>());
					    sectorRadiusContrastMap.get(sector).get(rad).add(contrast);
					    sectorRadiusContrastMap.get(sector).get(rad).add(sumLastSector);
					    
					}else if(sector!=8){
						b1 = b1*8/Math.PI;			
						b2 = b2*8/Math.PI;
						sum  = sum*4/Math.PI;
					    double b = Math.sqrt(b1*b1 + b2*b2);
					    double contrast = starType == StarTypeAdjustment.HARMONY ? (b/sum) :  (b/sum)*Math.PI/4;
					    if(!(sectorRadiusContrastMap.containsKey(sector))){
					    	sectorRadiusContrastMap.put(sector, new TreeMap<Double, ArrayList<Double>>());
					    }
					    double rad = N*imageHeight/(2*Math.PI*radius);
					    sectorRadiusContrastMap.get(sector).put(rad , new ArrayList<Double>());	
					    sectorRadiusContrastMap.get(sector).get(rad).add(contrast);
					    sectorRadiusContrastMap.get(sector).get(rad).add(sum);
					    b1 = 0.0;
						b2 = 0.0;
						sum = 0.0;
					}
				}
			}
		}else{
			Set<Double> radiusSet = starDataMap.keySet();
			for(Double radius : radiusSet){
				TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
				Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
				int sector = 0;
				int G = 1;      // nomer garmoniki
				double b1 = 0.0;   // perscha garmonika
				double b2 = 0.0;   // perscha garmonika
				double sum = 0.0;  // seredne znachenya
				
				double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;

				while(angle >= 0 && angle < (2*Math.PI) && angleIterator.hasNext()){	
					b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(G*N*angle);
					b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(G*N*angle);
					sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
					angle = angleIterator.next();
					sector = 1;
				}
				
				b1 = b1/Math.PI;			
				b2 = b2/Math.PI;
				sum  = sum/(2*Math.PI);
			    double b = Math.sqrt(b1*b1 + b2*b2);
			    double contrast = starType == StarTypeAdjustment.HARMONY ? (b/sum) : (b/sum)*Math.PI/4;      //for binary
			    if(!(sectorRadiusContrastMap.containsKey(sector))){
			    	sectorRadiusContrastMap.put(sector, new TreeMap<Double, ArrayList<Double>>());
			    }
			    double rad = N*imageHeight/(2*Math.PI*radius);
			    sectorRadiusContrastMap.get(sector).put(rad , new ArrayList<Double>());	
			    sectorRadiusContrastMap.get(sector).get(rad).add(contrast);
			    sectorRadiusContrastMap.get(sector).get(rad).add(sum);
			   
			}
		}
	}
	private void gatherSectorRadiusExMap(){
		Set<Double> radiusSet1 = starDataMap.keySet();
		if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
			for(Double radius : radiusSet1){
				TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
				Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
				int L = cyclesNum;
				int sector = 0;
				double mue = 0.0;
				double disp = 0.0;
				double Ex = 0.0;
				double mueLastSector = 0.0;
				double dispLastSector = 0.0;
				double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;
				while(angleIterator.hasNext()){	
	                
					while(angle >= 13*Math.PI/8 && angle < (15*Math.PI/8) && angleIterator.hasNext()){	
						
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(7).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(7).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 7;
					}
					while(angle >= 11*Math.PI/8 && angle < (13*Math.PI/8) && angleIterator.hasNext()){
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(6).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(6).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 6;
					}
					while(angle >= 9*Math.PI/8 && angle < (11*Math.PI/8) && angleIterator.hasNext()){	
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(5).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(5).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 5;
					}
					while(angle >= 7*Math.PI/8 && angle < (9*Math.PI/8) && angleIterator.hasNext()){	
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(4).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(4).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 4;
					}
					while(angle >= 5*Math.PI/8 && angle < (7*Math.PI/8) && angleIterator.hasNext()){	
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(3).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(3).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 3;
					}
					while(angle >= 3*Math.PI/8 && angle < (5*Math.PI/8) && angleIterator.hasNext()){	
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(2).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(2).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 2;
					}
					while(angle >= Math.PI/8 && angle < (3*Math.PI/8) && angleIterator.hasNext()){
						mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(1).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(1).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
						angle = angleIterator.next();
						sector = 1;
					}
					while(angleIterator.hasNext() && angle >= 15*Math.PI/8 && angle < 2*Math.PI || angle >= 0 && angle < Math.PI/8){	
						//System.out.println(sectorRadiusContrastMap.get(8).get(L*imageHeight/(2*Math.PI*radius)));
						mueLastSector = mueLastSector + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(8).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
						dispLastSector = dispLastSector + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(8).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						angle = angleIterator.next();
						sector = 8;
					}
					if(!angleIterator.hasNext()){
						mue = mue*4/Math.PI;			
						disp = Math.pow(disp, 2)*16/(Math.PI*Math.PI);
						Ex  = mue / disp - 1;
					    
					     if(!(sectorRadiusExMap.containsKey(sector))){
					    	sectorRadiusExMap.put(7, new TreeMap<Double, Double>());
					     }
					    sectorRadiusExMap.get(7).put(L*imageHeight/(2*Math.PI*radius), Ex);	
	
					    mueLastSector = mueLastSector*4/Math.PI;			
						dispLastSector = Math.pow(dispLastSector, 2)*16/(Math.PI*Math.PI);
						Ex  = mueLastSector / dispLastSector - 1;
						
					      if(!(sectorRadiusExMap.containsKey(sector))){
					    	sectorRadiusExMap.put(sector, new TreeMap<Double, Double>());
					      }
					    sectorRadiusExMap.get(sector).put(L*imageHeight/(2*Math.PI*radius), Ex);
	
					}else if(sector!=8){
						mue = mue*4/Math.PI;			
						disp = Math.pow(disp, 2)*16/(Math.PI*Math.PI);
						Ex  = mue / disp - 1;
					    if(!(sectorRadiusExMap.containsKey(sector))){
					    	sectorRadiusExMap.put(sector, new TreeMap<Double, Double>());
					    }
					    sectorRadiusExMap.get(sector).put(L*imageHeight/(2*Math.PI*radius), Ex);	
	
					    mue = 0.0;
						disp = 0.0;
						Ex = 0.0;
					}
				}
			}
		}else{
			for(Double radius : radiusSet1){
				TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
				Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
				int L = cyclesNum;
				int sector = 0;
				double mue = 0.0;
				double disp = 0.0;
				double Ex = 0.0;
				
				double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;
					
                    
				while(angle >= 0 && angle < (2*Math.PI) && angleIterator.hasNext()){	
						
					mue = mue + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(1).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 4) *(reworkableMap.higherKey(angle)-angle);
					disp = disp + Math.pow(reworkableMap.get(angle)*sectorRadiusContrastMap.get(1).get(L*imageHeight/(2*Math.PI*radius)).get(1) , 2) *(reworkableMap.higherKey(angle)-angle);
						
					angle = angleIterator.next();
					sector = 1;
					
				}
				mue = mue/(2*Math.PI);			
				disp = Math.pow(disp, 2)/(4*Math.PI*Math.PI);
				Ex  = mue / disp -1 ;
				if(!(sectorRadiusExMap.containsKey(sector))){
				   	sectorRadiusExMap.put(sector, new TreeMap<Double, Double>());
				}
				sectorRadiusExMap.get(sector).put(L*imageHeight/(2*Math.PI*radius), Ex);	
			}
		}
	}
	private void gatherSectorFourierSeriesMap(){
		int N = cyclesNum;
		
		Set<Double> radiusSet = starDataMap.keySet();
		if(sectorsNum == SectorsAdjustment.ALL_SECTORS){
			for(Double radius : radiusSet){
				 if (radius == starDataMap.lastKey() || radius == starDataMap.lastKey()-100|| radius == starDataMap.lastKey()-200 || radius == starDataMap.lastKey()-300){
					TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
					Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
					int sector = 0;
					double b1 = 0.0;   // perscha garmonika
					double b2 = 0.0;   // perscha garmonika
					double sum = 0.0;  // seredne znachenya
					double b1LastSector = 0;
					double b2LastSector = 0;
					double sumLastSector = 0;
					
					double b3 = 0.0;   // tretya garmonika
					double b4 = 0.0;   // tretya garmonika
					double b3LastSector = 0;
					double b4LastSector = 0;
					
					double b5 = 0.0;   // piyata garmonika
					double b6 = 0.0;   // piyata garmonika
					double b5LastSector = 0;
					double b6LastSector = 0;
					
					double b7 = 0.0;   // siyoma garmonika
					double b8 = 0.0;   // siyoma garmonika
					double b7LastSector = 0;
					double b8LastSector = 0;
					
					double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;
					while(angleIterator.hasNext()){	
                       
						while(angle >= 13*Math.PI/8 && angle < (15*Math.PI/8) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 7;
						}
						while(angle >= 11*Math.PI/8 && angle < (13*Math.PI/8) && angleIterator.hasNext()){
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 6;
						}
						while(angle >= 9*Math.PI/8 && angle < (11*Math.PI/8) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 5;
						}
						while(angle >= 7*Math.PI/8 && angle < (9*Math.PI/8) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 4;
						}
						while(angle >= 5*Math.PI/8 && angle < (7*Math.PI/8) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 3;
						}
						while(angle >= 3*Math.PI/8 && angle < (5*Math.PI/8) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 2;
						}
						while(angle >= Math.PI/8 && angle < (3*Math.PI/8) && angleIterator.hasNext()){
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 1;
						}
						while(angleIterator.hasNext() && angle >= 15*Math.PI/8 && angle < 2*Math.PI || angle >= 0 && angle < Math.PI/8){	
							b1LastSector = b1LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2LastSector = b2LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3LastSector = b3LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4LastSector = b4LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5LastSector = b5LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6LastSector = b6LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7LastSector = b7LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8LastSector = b8LastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sumLastSector = sumLastSector + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 8;
						}
						if(!angleIterator.hasNext()){
							b1 = b1*8/Math.PI;			
							b2 = b2*8/Math.PI;
							b3 = b3*8/Math.PI;			
							b4 = b4*8/Math.PI;
							b5 = b5*8/Math.PI;			
							b6 = b6*8/Math.PI;
							b7 = b7*8/Math.PI;			
							b8 = b8*8/Math.PI;
						    double b12 = Math.sqrt(b1*b1 + b2*b2);
						    double b34 = Math.sqrt(b3*b3 + b4*b4);
						    double b56 = Math.sqrt(b5*b5 + b6*b6);
						    double b78 = Math.sqrt(b7*b7 + b8*b8);
						    sum  = sum*4/Math.PI;
						    double contrast12 = (b12/sum)*1*Math.PI/4;      //for binary
						    double contrast34 = (b34/sum)*3*Math.PI/4;      //for binary
						    double contrast56 = (b56/sum)*5*Math.PI/4;      //for binary
						    double contrast78 = (b78/sum)*7*Math.PI/4;      //for binary
						     if(!(sectorFourierSeriesMap.containsKey(sector))){
						    	 sectorFourierSeriesMap.put(7, new TreeMap<Double, Double>());
						     }
						    double rad1 = 1*N*imageHeight/(2*Math.PI*radius);
						    double rad3 = 3*N*imageHeight/(2*Math.PI*radius);
						    double rad5 = 5*N*imageHeight/(2*Math.PI*radius);
						    double rad7 = 7*N*imageHeight/(2*Math.PI*radius);
						    sectorFourierSeriesMap.get(7).put(rad1, contrast12);
						    sectorFourierSeriesMap.get(7).put(rad3, contrast34);
						    sectorFourierSeriesMap.get(7).put(rad5, contrast56);
						    sectorFourierSeriesMap.get(7).put(rad7, contrast78);
						    
							
							b1LastSector = b1LastSector*8/Math.PI;			
							b2LastSector = b2LastSector*8/Math.PI;
							b3LastSector = b3LastSector*8/Math.PI;			
							b4LastSector = b4LastSector*8/Math.PI;
							b5LastSector = b5LastSector*8/Math.PI;			
							b6LastSector = b6LastSector*8/Math.PI;
							b7LastSector = b7LastSector*8/Math.PI;			
							b8LastSector = b8LastSector*8/Math.PI;
							sumLastSector  = sumLastSector*4/Math.PI;
						    b12 = Math.sqrt(b1LastSector*b1LastSector + b2LastSector*b2LastSector);
						    b34 = Math.sqrt(b3LastSector*b3LastSector + b4LastSector*b4LastSector);
						    b56 = Math.sqrt(b5LastSector*b5LastSector + b6LastSector*b6LastSector);
						    b78 = Math.sqrt(b7LastSector*b7LastSector + b8LastSector*b8LastSector);
						    contrast12 = (b12/sumLastSector)*1*Math.PI/4;     // for binary
						    contrast34 = (b34/sumLastSector)*3*Math.PI/4;     // for binary
						    contrast56 = (b56/sumLastSector)*5*Math.PI/4;     // for binary
						    contrast78 = (b78/sumLastSector)*7*Math.PI/4;     // for binary
						     if(!(sectorFourierSeriesMap.containsKey(sector))){
						    	sectorFourierSeriesMap.put(sector, new TreeMap<Double, Double>());
						     }
						    rad1 = 1*N*imageHeight/(2*Math.PI*radius);
						    rad3 = 3*N*imageHeight/(2*Math.PI*radius);
						    rad5 = 5*N*imageHeight/(2*Math.PI*radius);
						    rad7 = 7*N*imageHeight/(2*Math.PI*radius);
						    sectorFourierSeriesMap.get(sector).put(rad1, contrast12);
						    sectorFourierSeriesMap.get(sector).put(rad3, contrast34);
						    sectorFourierSeriesMap.get(sector).put(rad5, contrast56);
						    sectorFourierSeriesMap.get(sector).put(rad7, contrast78);
						    
						}else if(sector!=8){
							b1 = b1*8/Math.PI;			
							b2 = b2*8/Math.PI;
							b3 = b3*8/Math.PI;			
							b4 = b4*8/Math.PI;
							b5 = b5*8/Math.PI;			
							b6 = b6*8/Math.PI;
							b7 = b7*8/Math.PI;			
							b8 = b8*8/Math.PI;
						    double b12 = Math.sqrt(b1*b1 + b2*b2);
						    double b34 = Math.sqrt(b3*b3 + b4*b4);
						    double b56 = Math.sqrt(b5*b5 + b6*b6);
						    double b78 = Math.sqrt(b7*b7 + b8*b8);
						    sum  = sum*4/Math.PI;
						    double contrast12 = (b12/sum)*1*Math.PI/4;      //for binary
						    double contrast34 = (b34/sum)*3*Math.PI/4;      //for binary
						    double contrast56 = (b56/sum)*5*Math.PI/4;      //for binary
						    double contrast78 = (b78/sum)*7*Math.PI/4;      //for binary
						     if(!(sectorFourierSeriesMap.containsKey(sector))){
						    	 sectorFourierSeriesMap.put(sector, new TreeMap<Double, Double>());
						     }
						    double rad1 = 1*N*imageHeight/(2*Math.PI*radius);
						    double rad3 = 3*N*imageHeight/(2*Math.PI*radius);
						    double rad5 = 5*N*imageHeight/(2*Math.PI*radius);
						    double rad7 = 7*N*imageHeight/(2*Math.PI*radius);
						    sectorFourierSeriesMap.get(sector).put(rad1, contrast12);
						    sectorFourierSeriesMap.get(sector).put(rad3, contrast34);
						    sectorFourierSeriesMap.get(sector).put(rad5, contrast56);
						    sectorFourierSeriesMap.get(sector).put(rad7, contrast78);
						    b1 = 0.0;
							b2 = 0.0;
							b3 = 0.0;			
							b4 = 0.0;
							b5 = 0.0;			
							b6 = 0.0;
							b7 = 0.0;			
							b8 = 0.0;
							sum = 0.0;
						}
					}
				 }
			}
		}else{
			for(Double radius : radiusSet){
				 if (radius == starDataMap.lastKey() || radius == starDataMap.lastKey()-100|| radius == starDataMap.lastKey()-200 || radius == starDataMap.lastKey()-300){
					TreeMap<Double, Double> reworkableMap = starDataMap.get(radius);
					Iterator<Double> angleIterator = reworkableMap.keySet().iterator();
					int sector = 0;
					double b1 = 0.0;   // perscha garmonika
					double b2 = 0.0;   // perscha garmonika
					double sum = 0.0;  // seredne znachenya
					
					
					double b3 = 0.0;   // tretya garmonika
					double b4 = 0.0;   // tretya garmonika
					
					
					double b5 = 0.0;   // piyata garmonika
					double b6 = 0.0;   // piyata garmonika
					
					
					double b7 = 0.0;   // siyoma garmonika
					double b8 = 0.0;   // siyoma garmonika
					
					
					double angle = (angleIterator.hasNext()) ? angleIterator.next() : 0;
						
                   while(angle >= 0 && angle < (2*Math.PI) && angleIterator.hasNext()){	
							b1 = b1 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(1*N*angle);
							b2 = b2 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(1*N*angle);
							b3 = b3 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(3*N*angle);
							b4 = b4 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(3*N*angle);
							b5 = b5 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(5*N*angle);
							b6 = b6 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(5*N*angle);
							b7 = b7 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.sin(7*N*angle);
							b8 = b8 + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle)*Math.cos(7*N*angle);
							sum = sum + reworkableMap.get(angle)*(reworkableMap.higherKey(angle)-angle);
							angle = angleIterator.next();
							sector = 1;
					}
						
						
						
					b1 = b1/Math.PI;			
					b2 = b2/Math.PI;
					b3 = b3/Math.PI;			
					b4 = b4/Math.PI;
					b5 = b5/Math.PI;			
					b6 = b6/Math.PI;
					b7 = b7/Math.PI;			
					b8 = b8/Math.PI;
					double b12 = Math.sqrt(b1*b1 + b2*b2);
					double b34 = Math.sqrt(b3*b3 + b4*b4);
					double b56 = Math.sqrt(b5*b5 + b6*b6);
					double b78 = Math.sqrt(b7*b7 + b8*b8);
					sum  = sum/(2*Math.PI);
					double contrast12 = (b12/sum)*1*Math.PI/4;      //for binary
					double contrast34 = (b34/sum)*3*Math.PI/4;      //for binary
					double contrast56 = (b56/sum)*5*Math.PI/4;      //for binary
					double contrast78 = (b78/sum)*7*Math.PI/4;      //for binary
					if(!(sectorFourierSeriesMap.containsKey(sector))){
					  	 sectorFourierSeriesMap.put(sector, new TreeMap<Double, Double>());
					}
					double rad1 = 1*N*imageHeight/(2*Math.PI*radius);
					double rad3 = 3*N*imageHeight/(2*Math.PI*radius);
					double rad5 = 5*N*imageHeight/(2*Math.PI*radius);
					double rad7 = 7*N*imageHeight/(2*Math.PI*radius);
					sectorFourierSeriesMap.get(sector).put(rad1, contrast12);
					sectorFourierSeriesMap.get(sector).put(rad3, contrast34);
					sectorFourierSeriesMap.get(sector).put(rad5, contrast56);
					sectorFourierSeriesMap.get(sector).put(rad7, contrast78);
						   
						
				 }
			}
		}
		//System.out.println(sectorFourierSeriesMap.get(1));
	}

}
class MTFCalibrator{
	//private CalibrationDetailsListener calibrationDetails;
	private int bitDepth;
	private ImagePlus calibrationImage;
	
	private ArrayList<Double> calibRoi;
	private ArrayList<Double> calibRoiModified  = new ArrayList<Double>();
	private ArrayList<Double> calibRoiModDelta = new ArrayList<Double>();
	
	private ArrayList<Double> calibRef;
	private ArrayList<Double> calibRefModified  = new ArrayList<Double>();
	private ArrayList<Double> calibRefModDelta = new ArrayList<Double>();
	
	private ImageProcessor linearStarProc;
	
	
	MTFCalibrator (CalibrationDetailsListener calibrationDetails){
		//this.calibrationDetails = calibrationDetails;
		this.bitDepth = calibrationDetails.getBitDepth();
		this.calibrationImage = calibrationDetails.getCalibrationImage();
		//calibrationImage.setProcessor(calibrationImage.getProcessor().convertToByte(true));
		calibRoi = calibrationDetails.getSelectedColorValues();
		calibRef = calibrationDetails.getUserDefinedColorValues();
	}
	public ImagePlus calibrate(){	
		int maxIntencity = (int)Math.pow(2, bitDepth)-1;
		for(int i = 0; i < calibRoi.size(); i++){
			if(calibRoi.get(0) < calibRoi.get(calibRoi.size()-1)){
			double val = (calibRoi.get(i)-calibRoi.get(0))*(maxIntencity/(calibRoi.get(calibRoi.size()-1) - calibRoi.get(0)));
			calibRoiModified.add(val);
			val = (calibRef.get(0) - calibRef.get(i))*maxIntencity/(calibRef.get(0) - calibRef.get(calibRef.size()-1));
			calibRefModified.add(val);
			} else { 
				double val = maxIntencity - (calibRoi.get(i)-calibRoi.get(0))*(maxIntencity/(calibRoi.get(calibRoi.size()-1) - calibRoi.get(0)));
				calibRoiModified.add(val);
				val = maxIntencity - (calibRef.get(0) - calibRef.get(i))*maxIntencity/(calibRef.get(0) - calibRef.get(calibRef.size()-1));
				calibRefModified.add(val);
			}	
		} 
		for(int i = 0; i < calibRoiModified.size()-1; i++){
			calibRoiModDelta.add(calibRoiModified.get(i+1)-calibRoiModified.get(i));
			calibRefModDelta.add(calibRefModified.get(i+1)-calibRefModified.get(i));
		}
		linearStarProc = (ImageProcessor)calibrationImage.getProcessor();
		byte [] bPixels;
		short []  sPixels;
		double[] pixels;
		if(bitDepth == 16){
			sPixels = (short []) calibrationImage.getProcessor().getPixels();
			pixels = new double[sPixels.length];
			for(int i=0;i<sPixels.length;i++){
				pixels[i]= sPixels[i] & 0xffff;
			}
		}else{
			bPixels = (byte []) calibrationImage.getProcessor().getPixels();
			pixels = new double[bPixels.length];
			for(int i=0;i<bPixels.length;i++){
				pixels[i]= bPixels[i] & 0xff;
			}
		}
		
		
		
		for(int p = 0; p < pixels.length; p ++){
			
			if(calibRoi.get(0) < calibRoi.get(calibRoi.size()-1)){
				double x = maxIntencity/(calibRoi.get(calibRoi.size()-1)- calibRoi.get(0))*(pixels[p] - calibRoi.get(0));
				if(x <= 0){ 
					x=0;
				}else if(x >= maxIntencity){ 
					x = maxIntencity;
				}else{
					for(int i = 0; i < calibRoiModified.size()-1; i++){
						if(x >= calibRoiModified.get(i) && x < calibRoiModified.get(i+1)){
							x = calibRefModified.get(i) + (x - calibRoiModified.get(i))*calibRefModDelta.get(i)/calibRoiModDelta.get(i);
							break;
						}
					}
				}
				linearStarProc.set(p, (int)x);
			}else{
				double x = maxIntencity - maxIntencity/(calibRoi.get(calibRoi.size()-1)- calibRoi.get(0))*(pixels[p] - calibRoi.get(0));
				if(x <= 0){ 
					x=0;
				}else if(x >= maxIntencity){ 
					x = maxIntencity;
				}else{
					for(int i = 0; i < calibRoiModified.size()-1; i++){
						if(x <= calibRoiModified.get(i) && x > calibRoiModified.get(i+1)){
							x = calibRefModified.get(i) + (x - calibRoiModified.get(i))*calibRefModDelta.get(i)/calibRoiModDelta.get(i);
							break;
						}
					}
				}
				linearStarProc.set(p, (int)x);
			}
		}
		calibrationImage.setProcessor(linearStarProc);
		return calibrationImage;
	}
}
