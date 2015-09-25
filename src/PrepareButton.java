import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.esri.arcgis.addins.desktop.Button;
import com.esri.arcgis.arcmapui.IMxDocument;
import com.esri.arcgis.carto.FeatureLayer;
import com.esri.arcgis.carto.ILayer;
import com.esri.arcgis.carto.Map;
import com.esri.arcgis.carto.NetworkLayer;
import com.esri.arcgis.datasourcesGDB.InMemoryWorkspaceFactory;
import com.esri.arcgis.display.AlgorithmicColorRamp;
import com.esri.arcgis.display.IColor;
import com.esri.arcgis.display.ILineSymbol;
import com.esri.arcgis.display.IMarkerSymbol;
import com.esri.arcgis.display.IRgbColor;
import com.esri.arcgis.display.RgbColor;
import com.esri.arcgis.display.SimpleFillSymbol;
import com.esri.arcgis.display.SimpleLineSymbol;
import com.esri.arcgis.display.SimpleMarkerSymbol;
import com.esri.arcgis.display.esriColorRampAlgorithm;
import com.esri.arcgis.display.esriSimpleMarkerStyle;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.geodatabase.Field;
import com.esri.arcgis.geodatabase.Fields;
import com.esri.arcgis.geodatabase.GeometryDef;
import com.esri.arcgis.geodatabase.IEnumNetworkElement;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureBuffer;
import com.esri.arcgis.geodatabase.IFeatureClass;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.IFeatureWorkspace;
import com.esri.arcgis.geodatabase.IFeatureWorkspaceProxy;
import com.esri.arcgis.geodatabase.IGeometryDef;
import com.esri.arcgis.geodatabase.IGeometryDefEdit;
import com.esri.arcgis.geodatabase.INetworkEdge;
import com.esri.arcgis.geodatabase.INetworkElement;
import com.esri.arcgis.geodatabase.INetworkJunction;
import com.esri.arcgis.geodatabase.INetworkQuery;
import com.esri.arcgis.geodatabase.IQueryFilter;
import com.esri.arcgis.geodatabase.ISpatialFilter;
import com.esri.arcgis.geodatabase.IWorkspaceFactory;
import com.esri.arcgis.geodatabase.IWorkspaceProxy;
import com.esri.arcgis.geodatabase.NetworkDataset;
import com.esri.arcgis.geodatabase.SpatialFilter;
import com.esri.arcgis.geodatabase.esriFeatureType;
import com.esri.arcgis.geodatabase.esriFieldType;
import com.esri.arcgis.geodatabase.esriNetworkElementType;
import com.esri.arcgis.geodatabase.esriSpatialRelEnum;
import com.esri.arcgis.geometry.IControlPrecision2;
import com.esri.arcgis.geometry.IEnvelope;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.IProximityOperator;
import com.esri.arcgis.geometry.ISpatialReference;
import com.esri.arcgis.geometry.ISpatialReferenceFactory3;
import com.esri.arcgis.geometry.ITopologicalOperator;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.SpatialReferenceEnvironment;
import com.esri.arcgis.geometry.esriGeometryType;
import com.esri.arcgis.interop.AutomationException;
import com.esri.arcgis.system.Cleaner;
import com.esri.arcgis.system.IName;
import com.esri.arcgis.system.UID;


public class PrepareButton extends Button {

	public static class EdgeInfo {
		public DefaultWeightedEdge edge;
		public double lenght;
		public Double[] values;
		public EdgeInfo(DefaultWeightedEdge edge, double lenght, Double[] values) {
			super();
			this.edge = edge;
			this.lenght = lenght;
			this.values = values;
		}
	}
	public class FieldCost {
		public String name;
		public int index;
		public FieldCost(String name, int index) {
			super();
			this.name = name;
			this.index = index;
		}
	}

	public static final String DATA_TYPE = "Layer automatically generated. Scenario: ";
	public static SortedMap<String, FieldCost> fieldsCost;
	public static Set<String> scenarioNames = new HashSet<String>();
	public static String scenarioName;
	public static final HashMap<Integer, CalculateButton.RoadInfo> nearestRoads = new HashMap<Integer, CalculateButton.RoadInfo>();
	public static final int SIZE = 20;
	private static final String NA_TEXT = "---N/A---";
	private static boolean nextStep = false;
	private NetworkLayer networkLayer;
	private FeatureLayer roadsLayer;
	public static FeatureLayer locationsLayer;
	private IApplication app;
	public static double maxSize = -Double.MAX_VALUE;
	public static final String[] sizeColumn = new String[1];
	public static DefaultDirectedWeightedGraph<CalculateButton.NodeCoord, DefaultWeightedEdge>  graph;
	public static Collection<EdgeInfo> edgesInfo = new ArrayList<EdgeInfo>();
	public static FeatureLayer roadsCostLayer;
	public static FeatureLayer extraLayer;
	public static FeatureLayer locsCostLayer;
	public static Image icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB_PRE);
	
	@Override
	public void init(IApplication app) throws AutomationException, IOException {
		this.app = app;
	}
	/**
	 * Function to return the IColor object given the red, green and blue integers
	 * @param red
	 * @param green
	 * @param blue
	 * @return
	 */
	private static IColor getRGBColor(int red, int green, int blue) {
		RgbColor rgbColor = null;
		try {
			rgbColor = new RgbColor();
			rgbColor.setRed(red);
			rgbColor.setGreen(green);
			rgbColor.setBlue(blue);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rgbColor;
	}
	private static IColor getRGBAColor(int red, int green, int blue, byte a) {
		RgbColor rgbColor = null;
		try {
			rgbColor = new RgbColor();
			rgbColor.setRed(red);
			rgbColor.setGreen(green);
			rgbColor.setBlue(blue);
			rgbColor.setTransparency(a);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return rgbColor;
	}
	/**
	 * Called when the button is clicked.
	 * 
	 * @exception java.io.IOException if there are interop problems.
	 * @exception com.esri.arcgis.interop.AutomationException if the component throws an ArcObjects exception.
	 */
	@Override
	public void onClick() throws IOException, AutomationException {
		try {
			fieldsCost = new TreeMap<String, FieldCost>();
			ParametersButton.isEnabled = false;
			Map map = (Map) ((IMxDocument) app.getDocument()).getActiveView();
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			nextStep = false;
			
			//Layers window
			final JFrame windowL = new JFrame("Layers definition");
			windowL.setIconImage(icon);
			windowL.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			windowL.setLocationRelativeTo(null);
			windowL.setAlwaysOnTop(true);
			final java.util.Map<String, NetworkLayer> networkLayers = new HashMap<String, NetworkLayer>();
			final java.util.Map<String, FeatureLayer> lineLayers = new HashMap<String, FeatureLayer>();
			final java.util.Map<String, FeatureLayer> pointLayers = new HashMap<String, FeatureLayer>();
			for(int i=0; i<map.getLayerCount(); i++) {
				ILayer layer = map.getLayer(i); 
				if(layer instanceof NetworkLayer)
					networkLayers.put(layer.getName(), (NetworkLayer) layer);
				else if(layer instanceof FeatureLayer) {
					FeatureLayer featureLayer = (FeatureLayer)layer; 
					int shapeType = featureLayer.getFeatureClass().getShapeType();
					if(shapeType==esriGeometryType.esriGeometryLine || shapeType==esriGeometryType.esriGeometryPolyline)
						lineLayers.put(layer.getName(), featureLayer);
					else if(shapeType==esriGeometryType.esriGeometryPoint)
						pointLayers.put(layer.getName(), featureLayer);
				}
			}
			windowL.setLayout(new GridLayout(5/*2*/, 2));
			windowL.add(new JLabel("Scenario name"));
			final JTextField nameS= new JTextField("My scenario");
			windowL.add(nameS);
			windowL.add(new JLabel("Network layer"));
			final JComboBox netLs= new JComboBox(networkLayers.keySet().toArray());
			windowL.add(netLs);
			windowL.add(new JLabel("Walkways layer"));
			final JComboBox lineLs= new JComboBox(lineLayers.keySet().toArray());
			windowL.add(lineLs);
			windowL.add(new JLabel("Locations layer"));
			final JComboBox pointLs= new JComboBox(pointLayers.keySet().toArray());
			windowL.add(pointLs);
			JButton jButton = new JButton("Continue");
			jButton.addActionListener(new ActionListener() {	
				public void actionPerformed(ActionEvent e) {
					while(scenarioNames.contains(nameS.getText()))
						nameS.setText(nameS.getText()+"-copy");
					if(!nameS.getText().equals("") && netLs.getSelectedItem()!=null && lineLs.getSelectedItem()!=null) {
						scenarioName = nameS.getText();
						scenarioNames.add(scenarioName);
						networkLayer = networkLayers.get(netLs.getSelectedItem());
						roadsLayer = lineLayers.get(lineLs.getSelectedItem());
						locationsLayer = pointLayers.get(pointLs.getSelectedItem());
						nextStep = true;
						windowL.setVisible(false);
					}
				}
			});
			windowL.add(jButton);
			windowL.pack();
			windowL.setVisible(true);
			windowL.setFocusable(true);
			while(!nextStep)
				Thread.sleep(100);
			nextStep = false;
			//Fields window
			BufferedReader reader = new BufferedReader(new FileReader(System.getenv("AGSDESKTOPJAVA")+"bin\\Addins\\parameters.txt"));
			reader.readLine();
			Set<String> preBetas = new HashSet<String>();
			String line = reader.readLine();
			while(line!=null) {
				String[] parts = line.split("=");
				preBetas.add(parts[0].trim().toLowerCase());
				line = reader.readLine();
			}
			reader.close();
			final JFrame windowF = new JFrame("Cost related variables");
			windowF.setIconImage(icon);
			windowF.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			windowF.setLocationRelativeTo(null);
			windowF.setAlwaysOnTop(true);
			for(int i=0; i<roadsLayer.getFeatureClass().getFields().getFieldCount(); i++) {
				int type = roadsLayer.getFeatureClass().getFields().getField(i).getType();
				if(type==esriFieldType.esriFieldTypeDouble||type==esriFieldType.esriFieldTypeInteger||type==esriFieldType.esriFieldTypeSingle||type==esriFieldType.esriFieldTypeSmallInteger) {
					String nameF = roadsLayer.getFeatureClass().getFields().getField(i).getName().toLowerCase().trim();
					fieldsCost.put(nameF, new FieldCost(nameF, i));
				}
			}
			final JCheckBox[] checkFields = new JCheckBox[fieldsCost.size()];
			windowF.setLayout(new GridLayout(fieldsCost.size()+2, 1));
			windowF.add(new JLabel("Field"));
			int i=0;
			for(String field:fieldsCost.keySet()) {
				checkFields[i] = new JCheckBox(field,preBetas.contains(field));
				windowF.add(checkFields[i]);
				i++;
			}
			jButton = new JButton("Continue");
			jButton.addActionListener(new ActionListener() {	
				public void actionPerformed(ActionEvent e) {
					for(int i=0; i < checkFields.length; i++) {
						JCheckBox checkField = checkFields[i];
						if(!checkField.isSelected())
							fieldsCost.remove(checkField.getText());
					}
					nextStep = true;
					windowF.setVisible(false);
				}
			});
			windowF.add(jButton);
			windowF.pack();
			windowF.setVisible(true);
			while(!nextStep)
				Thread.sleep(100);
			nextStep = false;
			//Size Window
			final JFrame windowS = new JFrame("Entry size column");
			windowS.setIconImage(icon);
			windowS.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			windowS.setLocationRelativeTo(null);
			windowS.setAlwaysOnTop(true);
			Set<String> sizes = new HashSet<String>();
			sizes.add(NA_TEXT);
			for(int j=0; j<locationsLayer.getFeatureClass().getFields().getFieldCount(); j++) {
				int type = locationsLayer.getFeatureClass().getFields().getField(j).getType();
				if(type==esriFieldType.esriFieldTypeDouble||type==esriFieldType.esriFieldTypeInteger||type==esriFieldType.esriFieldTypeSingle||type==esriFieldType.esriFieldTypeSmallInteger)
					sizes.add(locationsLayer.getFeatureClass().getFields().getField(j).getName().toLowerCase().trim());
			}
			final JComboBox comboSizes = new JComboBox();
			for(String size:sizes)
				comboSizes.addItem(size);
			comboSizes.setSelectedItem(NA_TEXT);
			windowS.setLayout(new GridLayout(2, 2));
			windowS.add(new JLabel("Size column"));
			windowS.add(comboSizes);
			jButton = new JButton("Continue");
			jButton.addActionListener(new ActionListener() {	
				public void actionPerformed(ActionEvent e) {
					sizeColumn[0] = (String) comboSizes.getSelectedItem();	
					nextStep = true;
					windowS.setVisible(false);
				}
			});
			windowS.add(jButton);
			windowS.pack();
			windowS.setVisible(true);
			while(!nextStep)
				Thread.sleep(100);
			final JFrame windowP = new JFrame();
			windowP.setIconImage(icon);
			windowP.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			windowP.setAlwaysOnTop(true);
			windowP.setLocationRelativeTo(null);
			windowP.setUndecorated(true);
			JLabel lProgress = new JLabel();
			lProgress.setHorizontalAlignment(SwingConstants.LEFT);
			JProgressBar progressBar = new JProgressBar(0, 100);
			windowP.setLayout(new GridLayout(2, 1));
			progressBar.setValue(0);
			progressBar.setStringPainted(true);
			windowP.add(progressBar);
			windowP.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			windowP.add(lProgress);
			windowP.pack();
			windowP.setVisible(true);
			CalculateButton.exclusionSymbol = new SimpleLineSymbol();
			CalculateButton.exclusionSymbol.setColor(getRGBColor(200, 200, 200));
			CalculateButton.exclusionSymbol.setWidth(1);
			CalculateButton.exclusionSymbolL = new SimpleMarkerSymbol();
			CalculateButton.exclusionSymbolL.setColor(getRGBAColor(0, 0, 0, (byte) 0));
			CalculateButton.polygonSymbol1 = new SimpleFillSymbol();
			CalculateButton.polygonSymbol1.setColor(getRGBAColor(0, 0, 0, (byte) 0));
			SimpleLineSymbol lineSymbol1 = new SimpleLineSymbol();
			lineSymbol1.setColor(getRGBColor(100, 100, 100));
			lineSymbol1.setWidth(1.5);
			CalculateButton.polygonSymbol1.setOutline(lineSymbol1);
			CalculateButton.polygonSymbol2 = new SimpleFillSymbol();
			CalculateButton.polygonSymbol2.setColor(getRGBAColor(0, 0, 0, (byte) 0));
			SimpleLineSymbol lineSymbol2 = new SimpleLineSymbol();
			lineSymbol2.setColor(getRGBColor(50, 50, 50));
			lineSymbol2.setWidth(1.5);
			CalculateButton.polygonSymbol2.setOutline(lineSymbol2);
			CalculateButton.circleSymbol = new SimpleFillSymbol();
			CalculateButton.circleSymbol.setColor(getRGBAColor(0, 0, 0, (byte) 0));
			SimpleLineSymbol lineSymbolC = new SimpleLineSymbol();
			lineSymbolC.setColor(getRGBColor(150, 150, 150));
			lineSymbolC.setWidth(1.5);
			CalculateButton.circleSymbol.setOutline(lineSymbolC);
			AlgorithmicColorRamp ramp = new AlgorithmicColorRamp();
			ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
			IRgbColor fColor = new RgbColor();
			fColor.setRed(180);
			ramp.setFromColor(fColor);
			IRgbColor tColor = new RgbColor();
			tColor.setBlue(180);
			ramp.setToColor(tColor);
			ramp.setSize(SIZE);
			ramp.createRamp(new boolean[] { true });
			CalculateButton.locationSymbolsC = new IMarkerSymbol[SIZE];
			for(int v=0; v<SIZE; v++) {
				IMarkerSymbol symbolB = new SimpleMarkerSymbol();
				symbolB.setColor(ramp.getColor(v));
				CalculateButton.locationSymbolsC[v] = symbolB;
			}
			CalculateButton.locationSymbolsS = new IMarkerSymbol[SIZE];
			for(int v=0; v<SIZE; v++) {
				IMarkerSymbol symbolB = new SimpleMarkerSymbol();
				((SimpleMarkerSymbol)symbolB).setStyle(esriSimpleMarkerStyle.esriSMSCircle);
				symbolB.setSize(30*Math.sqrt((SIZE-v+15.0)/(SIZE+15.0)));
				CalculateButton.locationSymbolsS[v] = symbolB;
			}
			/*CalculateButton.locationSymbols = new ISymbol[SIZE][SIZE];
			for(int v=0; v<SIZE; v++) {
				for(int v2=0; v2<SIZE; v2++) {
					IMarkerSymbol symbolB = new SimpleMarkerSymbol();
					symbolB.setColor(ramp.getColor(v));
					((SimpleMarkerSymbol)symbolB).setStyle(esriSimpleMarkerStyle.esriSMSCircle);
					symbolB.setSize((SIZE-v2+40)*30.0/(SIZE+40));
					CalculateButton.locationSymbols[v][v2] = (ISymbol) symbolB;
				}
			}*/
			ramp = new AlgorithmicColorRamp();
			ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
			fColor = new RgbColor();
			fColor.setRed(255);
			ramp.setFromColor(fColor);
			tColor = new RgbColor();
			tColor.setBlue(255);
			ramp.setToColor(tColor);
			ramp.setSize(SIZE);
			ramp.createRamp(new boolean[] { true });
			CalculateButton.edgeSymbols = new ILineSymbol[SIZE];
			for(int v=0; v<SIZE; v++) {
				ILineSymbol symbolB = new SimpleLineSymbol();
				symbolB.setColor(ramp.getColor(v));
				symbolB.setWidth(1.5);
				CalculateButton.edgeSymbols[v] = symbolB;
			}
			lProgress.setText("Creating new layers");
			progressBar.setValue(1);
			IFeatureCursor cursor = roadsLayer.getFeatureClass().search(null, true);
			IFeature feature = cursor.nextFeature();
			int sizeI = 0;
			while(feature!=null) {
				if(feature.getOID()>sizeI)
					sizeI=feature.getOID();
				feature = cursor.nextFeature();
			}
			cursor.flush();
			progressBar.setValue(4);
			int sizeF = 0;
			for(FieldCost fieldCost:fieldsCost.values())
				if(fieldCost.index>sizeF)
					sizeF = fieldCost.index;
			progressBar.setValue(6);
			IGeometry[] shapes = new IGeometry[sizeI+1];
			Double[][] values = new Double[sizeI+1][sizeF+1];
			Integer[] networkIds = new Integer[sizeI+1];
			int numEdge=0;
			int numEdges = roadsLayer.getFeatureClass().featureCount(null);
			int nIndex = roadsLayer.getFeatureClass().findField("network_id");
			cursor = roadsLayer.getFeatureClass().search(null, true);
			feature = cursor.nextFeature();
			while(feature!=null) {
				Polyline p = new Polyline();
				p.addPointCollection((Polyline)feature.getShape());
				shapes[feature.getOID()] =  p;
				for(FieldCost fieldCost:fieldsCost.values())
					values[feature.getOID()][fieldCost.index] = feature.getValue(fieldCost.index) instanceof Double?(Double)feature.getValue(fieldCost.index):feature.getValue(fieldCost.index) instanceof Integer?((Integer)feature.getValue(fieldCost.index)).doubleValue():((Short)feature.getValue(fieldCost.index)).doubleValue();
				Object object = feature.getValue(nIndex);
				networkIds[feature.getOID()] = object instanceof Double?((Double)object).intValue():object instanceof Short?((Short)object).intValue():(Integer)object;
				feature = cursor.nextFeature();
				progressBar.setValue(6+(32*++numEdge/numEdges));
			}
			cursor.flush();
			IWorkspaceFactory workspaceFactory = new InMemoryWorkspaceFactory();
			IFeatureWorkspace outputfeatureWorkspace = new IFeatureWorkspaceProxy(new IWorkspaceProxy(((IName) workspaceFactory.create("", "MyWorkspace", null, 0)).open()));
			Fields fields = new Fields();
			Field gField = new Field();
			IGeometryDef geometryDef = new GeometryDef();
			IGeometryDefEdit geometryDefEdit = (IGeometryDefEdit)geometryDef;
			geometryDefEdit.setGeometryType(esriGeometryType.esriGeometryPolyline);
			geometryDefEdit.setSpatialReferenceByRef(networkLayer.getSpatialReference()); 
			gField.setName("Shape");
			gField.setType(esriFieldType.esriFieldTypeGeometry);
			gField.setGeometryDefByRef(geometryDefEdit);
			fields.addField(gField);
			Field field = new Field();
			field.setName("PathCost");
			field.setType(esriFieldType.esriFieldTypeDouble);
			fields.addField(field);
			field = new Field();
			field.setName("Cost");
			field.setType(esriFieldType.esriFieldTypeDouble);
			fields.addField(field);
			field = new Field();
			field.setName("OriginId");
			field.setType(esriFieldType.esriFieldTypeInteger);
			fields.addField(field);
			field = new Field();
			field.setName("NetworkId");
			field.setType(esriFieldType.esriFieldTypeInteger);
			fields.addField(field);
			IFeatureClass featureClass = outputfeatureWorkspace.createFeatureClass("edges", fields, null, new UID(), esriFeatureType.esriFTSimple, "Shape", "");
			//Cursor to insert each road as a feature
			cursor = featureClass.IFeatureClass_insert(true);
			IFeatureBuffer featureBuffer = featureClass.createFeatureBuffer();
			NetworkDataset n = new NetworkDataset(networkLayer.getNetworkDataset());
			INetworkQuery networkQuery = (INetworkQuery)n;
			IEnumNetworkElement edges = networkQuery.getElements(esriNetworkElementType.esriNETEdge);
			numEdges = networkQuery.getElementCount(esriNetworkElementType.esriNETEdge);
			INetworkEdge edge = (INetworkEdge) edges.next();
			//In this loop vertexes and edges are added to the graph and features are added to the new feature class
			lProgress.setText("Creating graph");
			graph = new DefaultDirectedWeightedGraph<CalculateButton.NodeCoord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
			numEdge=0;
			int newNumEdges = 0;
			progressBar.setValue(40);
			while(edge != null) {
				INetworkJunction nodeF = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction), nodeT = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction);
				edge.queryJunctions(nodeF, nodeT);
				IPoint pF = new Point();
				nodeF.queryPoint(pF);
				IPoint pT = new Point();
				nodeT.queryPoint(pT);
				CalculateButton.Coord cF = new CalculateButton.Coord(pF.getX(),pF.getY());
				CalculateButton.Coord cT = new CalculateButton.Coord(pT.getX(),pT.getY());
				CalculateButton.NodeCoord nCF = new CalculateButton.NodeCoord(cF,null);
				CalculateButton.NodeCoord nCT = new CalculateButton.NodeCoord(cT,null);
				graph.addVertex(nCF);
				graph.addVertex(nCT);
				DefaultWeightedEdge edgeN = graph.addEdge(nCF, nCT);
				if(edgeN!=null) {
					IGeometry p = shapes[edge.getOID()];
					edgesInfo.add(new EdgeInfo(edgeN, ((Polyline)p).getLength(), values[edge.getOID()]));
					graph.setEdgeWeight(edgeN, ((Polyline)p).getLength());
					featureBuffer.setShapeByRef(p);
					int oId = (Integer)cursor.insertFeature(featureBuffer);
					CalculateButton.roadsInfo.put(oId, new CalculateButton.RoadInfo(nCF, nCT, p, edge.getOID(),networkIds[edge.getOID()]));
					newNumEdges++;
				}
				edgeN = graph.addEdge(nCT, nCF);
				if(edgeN!=null) {
					IGeometry p = shapes[edge.getOID()];
					edgesInfo.add(new EdgeInfo(edgeN, ((Polyline)p).getLength(), values[edge.getOID()]));
					graph.setEdgeWeight(edgeN, ((Polyline)p).getLength());
				}
				INetworkElement e = edges.next();
				while(e != null && !(e instanceof INetworkEdge))
					e = edges.next();
				edge = (INetworkEdge) e;
				progressBar.setValue(40+(2*(++numEdge*60)/numEdges));
			}
			cursor.flush();
				
			//New layer is created and added to the map
			roadsCostLayer = new FeatureLayer();
			roadsCostLayer.setName(scenarioName+"_paths");
			roadsCostLayer.setDataSourceType(DATA_TYPE+scenarioName);
			roadsCostLayer.setFeatureClassByRef(featureClass);
			
			//New layer for extra results
			fields = new Fields();
			gField = new Field();
			geometryDef = new GeometryDef();
			geometryDefEdit = (IGeometryDefEdit)geometryDef;
			geometryDefEdit.setGeometryType(esriGeometryType.esriGeometryPolygon);
			geometryDefEdit.setSpatialReferenceByRef(networkLayer.getSpatialReference()); 
			gField.setName("Shape");
			gField.setType(esriFieldType.esriFieldTypeGeometry);
			gField.setGeometryDefByRef(geometryDefEdit);
			fields.addField(gField);
			field = new Field();
			field.setName("Number");
			field.setType(esriFieldType.esriFieldTypeInteger);
			fields.addField(field);
			IFeatureClass featureClassE = outputfeatureWorkspace.createFeatureClass("extra", fields, null, new UID(), esriFeatureType.esriFTSimple, "Shape", "");
			extraLayer = new FeatureLayer();
			extraLayer.setName(scenarioName+"_results");
			extraLayer.setDataSourceType(DATA_TYPE+scenarioName);
			extraLayer.setFeatureClassByRef(featureClassE);
			
			fields = new Fields();
			gField = new Field();
			geometryDef = new GeometryDef();
			geometryDefEdit = (IGeometryDefEdit)geometryDef;
			geometryDefEdit.setGeometryType(esriGeometryType.esriGeometryPoint);
			geometryDefEdit.setSpatialReferenceByRef(networkLayer.getSpatialReference()); 
			gField.setName("Shape");
			gField.setType(esriFieldType.esriFieldTypeGeometry);
			gField.setGeometryDefByRef(geometryDefEdit);
			fields.addField(gField);
			field = new Field();
			field.setName("PathCost");
			field.setType(esriFieldType.esriFieldTypeDouble);
			field.setDefaultValue(Double.POSITIVE_INFINITY);
			fields.addField(field);
			field = new Field();
			field.setName("PathCostCat");
			field.setType(esriFieldType.esriFieldTypeInteger);
			field.setDefaultValue(-1);
			fields.addField(field);
			field = new Field();
			field.setName("OriginId");
			field.setType(esriFieldType.esriFieldTypeInteger);
			fields.addField(field);
			field = new Field();
			field.setName("Size");
			field.setType(esriFieldType.esriFieldTypeDouble);
			fields.addField(field);
			IFeatureClass featureClassL = outputfeatureWorkspace.createFeatureClass("locs", fields, null, new UID(), esriFeatureType.esriFTSimple, "Shape", "");
			//Cursor to insert each road as a feature
			lProgress.setText("Snapping access points to graph");
			locsCostLayer = new FeatureLayer();
			locsCostLayer.setName(scenarioName+"_entries");
			locsCostLayer.setDataSourceType(DATA_TYPE+scenarioName);
			locsCostLayer.setFeatureClassByRef(featureClassL);
			int f=0;
			int sizeIndex = sizeColumn[0].equals(NA_TEXT)?-1:locationsLayer.getFeatureClass().findField(sizeColumn[0]);
			cursor = locationsLayer.getFeatureClass().search(null, true);
			feature = cursor.nextFeature();
			maxSize = -Double.MAX_VALUE;
			while(feature!=null) {
				progressBar.setValue(200*f++/newNumEdges);
				int nearestOID = Integer.MIN_VALUE;
				double searchRadius = 20;
				while(nearestOID == Integer.MIN_VALUE) {
					nearestOID = findNearest(feature.getShape(), featureClass, searchRadius);
					searchRadius *= 2;
				}
				CalculateButton.RoadInfo roadInfo = CalculateButton.roadsInfo.get(nearestOID);
				IPoint p = new Point();
				p.setX(((IPoint)feature.getShape()).getX());
				p.setY(((IPoint)feature.getShape()).getY());
				double size = 1;
				if(sizeIndex!=-1) {
					size = (Double) feature.getValue(sizeIndex);
					if(size>maxSize )
						maxSize = size;
				}
				roadInfo.locs.add(new CalculateButton.RelAccess(feature.getOID(), p, size));
				/*if(feature.getOID()==2 || feature.getOID()==8)
					JOptionPane.showMessageDialog(null, ((IPoint)roadInfo.locs.get(0).p).getX()+","+((IPoint)roadInfo.locs.get(0).p).getY());*/
				nearestRoads.put(feature.getOID(), roadInfo);
				feature = cursor.nextFeature();
			}
			//map.addLayer(roadsCostLayer);
			//roadsCostLayer.setVisible(false);
			Toolkit.getDefaultToolkit().beep();
			windowP.setCursor(null);
			windowP.setVisible(false);
			for(int k=0; k<((IMxDocument)app.getDocument()).getContentsViewCount(); k++)
				((IMxDocument)app.getDocument()).getContentsView(k).refresh(null);
			map.refresh();
			ParametersButton.isEnabled = true;
		} catch (AutomationException e1) {
			/*for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
			JOptionPane.showMessageDialog(null, e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(null, e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
		} catch (NullPointerException e1) {
			/*for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
			JOptionPane.showMessageDialog(null, "null: "+e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
		} catch (Exception e1) {
			/*for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());*/
			JOptionPane.showMessageDialog(null, "other: "+e1.getClass().toString()+" "+e1.getMessage()+" "+e1.getStackTrace()[0].getLineNumber());
		}
	}
	public ISpatialReference convertSpatialReferenceFromHighToLowPrecision(ISpatialReference highSpatialReference, IEnvelope envelope) throws AutomationException, IOException
	{
		IControlPrecision2 controlPrecision2 = (IControlPrecision2) highSpatialReference;
		if (!controlPrecision2.isHighPrecision())
			throw new RuntimeException("Expected a high precision spatial reference.");
		ISpatialReference lowSpatialReference = null;
		ISpatialReferenceFactory3 spatialReferenceFactory3 = new SpatialReferenceEnvironment();
		try {
			lowSpatialReference = spatialReferenceFactory3.constructLowPrecisionSpatialReference(true, highSpatialReference, envelope);
		} catch (Exception e) {
			lowSpatialReference = spatialReferenceFactory3.constructLowPrecisionSpatialReference(false, highSpatialReference, envelope);
		}
		return lowSpatialReference;
	}

	private int findNearest(IGeometry g, IFeatureClass flayer, double searchradius) throws AutomationException, IOException
	{
		IFeatureCursor fcursor = doSpatialQuery(flayer, g, searchradius);
		int returnval = findNearest(g, fcursor);
		Cleaner.release(fcursor);
		return returnval;
	}

	///<summary>
	/// Creates a spatial query which performs a spatial search for 
	/// features in the supplied feature class and has the option to also apply an attribute query via a where clause.
	/// See http://edndoc.esri.com/arcobjects/9.2/NET/7b4b8987-a3f0-4954-980f-720e61965449.htm
	/// By accepting an IFeatureLayer (instead of IFeatureClass) any definition query on the layer will be honored, and we have the option to honor any preexisting selection set.
	/// </summary>
	private IFeatureCursor doSpatialQuery(IFeatureClass flayer, IGeometry searchGeometry, double bufferdistance) throws AutomationException, IOException
	{
		ISpatialFilter spatialFilter = CreateSpatialFilter(flayer, searchGeometry, esriSpatialRelEnum.esriSpatialRelIntersects, bufferdistance);
		// perform the query and use a cursor to hold the results
		IFeatureCursor featureCursor = flayer.search((IQueryFilter)spatialFilter, true);
		Cleaner.release(spatialFilter);
		return featureCursor;
	}

	/// <summary>
	/// Loops over rows in a FeatureCursor, measuring the distance from 
	/// the target Geometry to each feature in the FeatureCursor, to
	/// find the closest one.
	/// </summary>
	private int findNearest(IGeometry g, IFeatureCursor fcursor) throws AutomationException, IOException
	{
		int closestoid = Integer.MIN_VALUE;
		double closestdistance = 0.0;
		IFeature feature = fcursor.nextFeature();
		while (feature != null)
		{
			double distance = ((IProximityOperator)g).returnDistance(feature.getShape());
			if (closestoid == Integer.MIN_VALUE || distance < closestdistance)
			{
				closestoid = feature.getOID();
				closestdistance = distance;
			}
			feature = fcursor.nextFeature();
			
		}
		return closestoid;
	}

	/// <summary>
	/// Utility function to create a spatial filter.
	/// </summary>
	private ISpatialFilter CreateSpatialFilter(IFeatureClass flayer, IGeometry searchGeometry, int spatialRelation, double bufferdistance) throws AutomationException, IOException
	{
		// create a spatial query filter
		ISpatialFilter spatialFilter = new SpatialFilter();
		spatialFilter.setGeometryField(flayer.getShapeFieldName());
		spatialFilter.setSpatialRel(spatialRelation);

		// specify the geometry to query with. apply a buffer if desired
		if (bufferdistance > 0.0)
		{
			// Use the ITopologicalOperator interface to create a buffer.
			ITopologicalOperator topoOperator = (ITopologicalOperator)searchGeometry;
			IGeometry buffer = topoOperator.buffer(bufferdistance);
			spatialFilter.setGeometryByRef(buffer);
		}
		else
			spatialFilter.setGeometryByRef(searchGeometry);

		//limit the fields included on the cursor to those needed 
		//(you don't need to include columns used in the where clause)
		spatialFilter.setSubFields(flayer.getOIDFieldName()+","+flayer.getShapeFieldName());

		return spatialFilter;
	}

}
