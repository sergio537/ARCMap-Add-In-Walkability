import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.NumberFormatter;

import org.jgrapht.alg.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import com.esri.arcgis.addins.desktop.DockableWindow;
import com.esri.arcgis.arcmapui.IMxDocument;
import com.esri.arcgis.carto.CircleElement;
import com.esri.arcgis.carto.CompositeGraphicsLayer;
import com.esri.arcgis.carto.FeatureLayer;
import com.esri.arcgis.carto.LineElement;
import com.esri.arcgis.carto.Map;
import com.esri.arcgis.carto.MapSelection;
import com.esri.arcgis.carto.NetworkLayer;
import com.esri.arcgis.display.AlgorithmicColorRamp;
import com.esri.arcgis.display.IColor;
import com.esri.arcgis.display.IFillSymbol;
import com.esri.arcgis.display.ILineSymbol;
import com.esri.arcgis.display.IRgbColor;
import com.esri.arcgis.display.RgbColor;
import com.esri.arcgis.display.SimpleFillSymbol;
import com.esri.arcgis.display.SimpleLineSymbol;
import com.esri.arcgis.display.esriColorRampAlgorithm;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.geodatabase.IEnumNetworkElement;
import com.esri.arcgis.geodatabase.IFeature;
import com.esri.arcgis.geodatabase.IFeatureCursor;
import com.esri.arcgis.geodatabase.INetworkEdge;
import com.esri.arcgis.geodatabase.INetworkElement;
import com.esri.arcgis.geodatabase.INetworkJunction;
import com.esri.arcgis.geodatabase.INetworkQuery;
import com.esri.arcgis.geodatabase.NetworkDataset;
import com.esri.arcgis.geodatabase.esriNetworkElementType;
import com.esri.arcgis.geometry.CircularArc;
import com.esri.arcgis.geometry.IGeometry;
import com.esri.arcgis.geometry.IPoint;
import com.esri.arcgis.geometry.IPolyline;
import com.esri.arcgis.geometry.ISegment;
import com.esri.arcgis.geometry.ISegmentCollection;
import com.esri.arcgis.geometry.Point;
import com.esri.arcgis.geometry.Polygon;
import com.esri.arcgis.geometry.Polyline;
import com.esri.arcgis.geometry.Ring;
import com.esri.arcgis.interop.AutomationException;

public class DockableWindowSergio2 extends DockableWindow {

	private JPanel jPanel;
	private int sizeColors;
	private Map map;
	private Set<EdgeInfo> edgeElements;
	private ILineSymbol[] edgeSymbols;
	private IFillSymbol[] pointSymbols;
	private DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge> graph;
	private JTextField textMax;
	private JTextField textLenght;
	private JTextField textAngle;
	private int lenghtBeta = 0;
	private int angleBeta = 0;
	
	private static class Coord {
		private double x;
		private double y;

		public Coord(double x, double y) {
			super();
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode() {
			return (int) (x*y);
		}

		@Override
		public String toString() {
			return x+" , "+y;
		}

		@Override
		public boolean equals(Object o) {
			if(o instanceof Coord)
				if(((Coord)o).x==x && ((Coord)o).y==y)
					return true;
			return false;
		}
	}

	private class EdgeInfo {
		private LineElement line;
		private Coord cF;
		private Coord cT;
		//private Collection<CircleElement> circles = new ArrayList<CircleElement>();
		public EdgeInfo(LineElement line, Coord cF, Coord cT) {
			super();
			this.line = line;
			this.cF = cF;
			this.cT = cT;
		}
		/*public void addCircle(CircleElement circle) {
			circles.add(circle);
		}*/
	}

	private IColor getRGBColor(int red, int green, int blue) {
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

	@Override
	public void init(final IApplication app) {
		try {
			SimpleLineSymbol edgeSymbol = new SimpleLineSymbol();
			edgeSymbol.setColor(getRGBColor(200, 200, 200));
			edgeSymbol.setWidth(1);
			SimpleFillSymbol fillSymbol = new SimpleFillSymbol();
			fillSymbol.setColor(getRGBColor(0, 0, 50));
			map = (Map) ((IMxDocument) app.getDocument()).getActiveView();
			AlgorithmicColorRamp ramp = new AlgorithmicColorRamp();
			ramp.setAlgorithm(esriColorRampAlgorithm.esriHSVAlgorithm);
			IRgbColor fColor = new RgbColor();
			fColor.setRed(255);
			ramp.setFromColor(fColor);
			IRgbColor tColor = new RgbColor();
			tColor.setBlue(255);
			ramp.setToColor(tColor);
			sizeColors = 20;
			ramp.setSize(sizeColors);
			ramp.createRamp(new boolean[] { true });
			edgeSymbols = new ILineSymbol[sizeColors+1];
			pointSymbols = new IFillSymbol[sizeColors+1];
			for(int v=0; v<sizeColors; v++) {
				ILineSymbol symbolE = new SimpleLineSymbol();
				symbolE.setColor(ramp.getColor(v));
				symbolE.setWidth(1.5);
				edgeSymbols[v] = symbolE;
				IFillSymbol symbolP = new SimpleFillSymbol();
				symbolP.setColor(ramp.getColor(v));
				pointSymbols[v] = symbolP;
			}
			edgeSymbols[sizeColors] = edgeSymbol;
			pointSymbols[sizeColors] = fillSymbol;
			NetworkLayer roadsLayer = null;
			FeatureLayer buildingsLayer = null;
			for(int i=0; i<map.getLayerCount(); i++) {
				if(map.getLayer(i) instanceof NetworkLayer)
					roadsLayer = (NetworkLayer) map.getLayer(i);
				else if(map.getLayer(i).getName().equals("access_points_entrances"))
					buildingsLayer = (FeatureLayer) map.getLayer(i);
			}
			graph = new DefaultDirectedWeightedGraph<Coord, DefaultWeightedEdge>(DefaultWeightedEdge.class);
			NetworkDataset n = new NetworkDataset(roadsLayer.getNetworkDataset());
			INetworkQuery networkQuery = (INetworkQuery)n;
			IEnumNetworkElement edges = networkQuery.getElements(esriNetworkElementType.esriNETEdge);
			INetworkEdge edge = (INetworkEdge) edges.next();
			edgeElements = new HashSet<EdgeInfo>();
			CompositeGraphicsLayer roads = new CompositeGraphicsLayer();
			roads.setName("roads");
			map.addLayer(roads);
			CompositeGraphicsLayer buildings = new CompositeGraphicsLayer();
			buildings.setName("buildings");
			map.addLayer(buildings);
			//map.setActiveGraphicsLayerByRef(new GraphicsSubLayer(((ICompositeGraphicsLayer)map.getBasicGraphicsLayer()).addLayer("paths", null)));
			while(edge != null) {
				INetworkJunction nodeF = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction), nodeT = (INetworkJunction) networkQuery.createNetworkElement(esriNetworkElementType.esriNETJunction);
				edge.queryJunctions(nodeF, nodeT);
				IPoint pF = new Point();
				nodeF.queryPoint(pF);
				Coord cF = new Coord(pF.getX(),pF.getY());
				graph.addVertex(cF);
				IPoint pT = new Point();
				nodeT.queryPoint(pT);
				Coord cT = new Coord(pT.getX(),pT.getY());
				graph.addVertex(cT);
				graph.addEdge(cT, cF);
				DefaultWeightedEdge edgeN = graph.addEdge(cF, cT);
				if(edgeN!=null && Math.random()<2) {
					IPolyline l = new Polyline();
					l.setFromPoint(pF);
					l.setToPoint(pT);
					LineElement elementL = new LineElement();
					elementL.setGeometry(l);
					elementL.setSymbol(edgeSymbol);
					EdgeInfo info = new EdgeInfo(elementL, cF, cT);
					edgeElements.add(info);
					//((IGraphicsContainer)map.getFocusMap()).addElement(elementL, 0);
					roads.addElement(elementL, 0);
				}
				INetworkElement e = edges.next();
				while(e != null && !(e instanceof INetworkEdge))
					e = edges.next();
				edge = (INetworkEdge) e;
			}
			IFeatureCursor cursor = buildingsLayer.getFeatureClass().search(null, true);
			IFeature feature = cursor.nextFeature();
			int index = buildingsLayer.getFeatureClass().findField("PARCEL_SIZ");
			while(feature!=null) {
				if(Math.random()<2) {
					CircleElement circleElement = new CircleElement();
					CircularArc circle = new CircularArc();
					circle.constructCircle((IPoint)feature.getShape(), 2.5*Math.log((Double)feature.getValue(index)), false);
					ISegmentCollection ring1 = new Ring();
				    ring1.addSegment((ISegment)circle, null, null);
					Polygon pol = new Polygon();
					pol.addGeometry((IGeometry)ring1, null, null);
					circleElement.setGeometry(pol);
					circleElement.setSymbol(fillSymbol);
					buildings.addElement(circleElement, 0);
					/*EdgeInfo closest = edgeElements.iterator().next();
					for(EdgeInfo info:edgeElements)
						if(getDistance(info.line, circleElement)<getDistance(closest.line, circleElement))
							closest=info;
					closest.addCircle(circleElement);*/
				}
				feature = cursor.nextFeature();
			}
			map.setExpanded(true);
			JOptionPane.showMessageDialog(jPanel, "Nodes: "+graph.vertexSet().size()+", Links: "+graph.edgeSet().size());
		} catch (AutomationException e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, e1.getMessage());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, e.getMessage());
		} catch (NullPointerException e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, "null: "+e1.getMessage());
		} catch (Exception e1) {
			for(StackTraceElement trace:e1.getStackTrace())
				JOptionPane.showMessageDialog(null, "trace: "+trace.getLineNumber()+" "+trace.getMethodName()+" "+trace.getClassName());
			JOptionPane.showMessageDialog(null, "other: "+e1.getClass().toString()+" "+e1.getMessage());
		}
	}
	/*private double getDistance(LineElement lineElement, CircleElement circleElement) throws AutomationException, IOException {
		return ((Polyline)lineElement.getGeometry()).returnDistance(circleElement.getGeometry());
	}*/

	@Override
	public Component createUI() {
		jPanel = new JPanel();
		jPanel.setLayout(new BorderLayout());
		JPanel jPanelButtons = new JPanel();
		jPanelButtons.setLayout(new GridLayout(2, 1));
		confButtonP(jPanelButtons);
		confButtonN(jPanelButtons);
		jPanel.add(jPanelButtons, BorderLayout.NORTH);
		return jPanel;
	}
	private void confButtonP(JPanel jPanelButtons) {
		final JButton jButton = new JButton("Parametes");
		final JDialog dialog = new JDialog((Frame) jPanelButtons.getParent(), "Paramaters", true);
		JPanel jPanelParams = new JPanel();
		jPanelParams.setLayout(new GridLayout(6, 1));
		jPanelParams.add(new JLabel("Max Cost"));
		NumberFormat format = NumberFormat.getInstance();
		NumberFormatter formatter = new NumberFormatter(format);
		formatter.setValueClass(Integer.class);
		formatter.setMinimum(0);
		formatter.setMaximum(Integer.MAX_VALUE);
		formatter.setCommitsOnValidEdit(true);
		textMax = new JFormattedTextField(formatter);
		textMax.setText("10");
		textMax.setEditable(true);
		textMax.setEnabled(true);
		jPanelParams.add(textMax);
		jPanelParams.add(new JLabel("Lenght 1-10"));
		textLenght = new JFormattedTextField(formatter);
		textLenght.setText("10");
		textLenght.setEditable(true);
		textLenght.setEnabled(true);
		jPanelParams.add(textLenght);
		jPanelParams.add(new JLabel("Angle 1-10"));
		textAngle = new JFormattedTextField(formatter);
		textAngle.setText("0");
		textAngle.setEditable(true);
		textAngle.setEnabled(true);
		jPanelParams.add(textAngle);
		dialog.setLayout(new BorderLayout());
		dialog.add(jPanelParams, BorderLayout.CENTER);
		dialog.pack();
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.setLocationRelativeTo(jButton);
				dialog.setVisible(true);
			}
		});
		jPanelButtons.add(jButton);
	}
	private void confButtonN(final JPanel jPanel) {
		JButton jButton = new JButton("Replacing colors");
		jButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					if (map.getSelectionCount() == 1)
						if(!textMax.getText().isEmpty() && !textLenght.getText().isEmpty() && !textAngle.getText().isEmpty()) {
							long[] times = new long[9];
							MapSelection mapSelection = new MapSelection(map.getFeatureSelection());
							Point feature = (Point)mapSelection.next().getShape();
							Coord c = new Coord(feature.getX(), feature.getY());
							times[6] -= System.currentTimeMillis();
							BellmanFordShortestPath<Coord, DefaultWeightedEdge> algo = new BellmanFordShortestPath<Coord, DefaultWeightedEdge>(graph, c);
							times[6] += System.currentTimeMillis();
							double max = new Double(textMax.getText());
							int lenghtB = new Integer(textLenght.getText());
							int angleB = new Integer(textAngle.getText());
							if(lenghtB!=lenghtBeta || angleB!=angleBeta) {
								times[4] -= System.currentTimeMillis();
								for(DefaultWeightedEdge edge:graph.edgeSet())
									graph.setEdgeWeight(edge, getCost(lenghtB, angleB, edge));
								times[4] += System.currentTimeMillis();
								lenghtBeta = lenghtB;
								angleBeta = angleB;
							}
							times[5] -= System.currentTimeMillis();
							for(EdgeInfo element:edgeElements) {
								double fCost = element.cF.equals(c)?0:algo.getCost(element.cF);
								double tCost = element.cT.equals(c)?0:algo.getCost(element.cT);
								double cost = (fCost+tCost)/2;
								int v = cost>max?sizeColors:(int)(cost*(sizeColors - 1)/max);
								element.line.setSymbol(edgeSymbols[v]);
								/*for(CircleElement circleElement:element.circles)
									circleElement.setSymbol(pointSymbols[v]);*/
							}
							times[5] += System.currentTimeMillis();
							map.setExpanded(true);
							map.refresh();
							JOptionPane.showMessageDialog(jPanel, times[0]/edgeElements.size()+" "+times[1]/edgeElements.size()+" "+times[2]/edgeElements.size()+" "+times[3]/edgeElements.size()+" "+times[4]+" "+times[5]+" "+times[6]+" "+times[7]+" "+times[8]);
						}
						else
							JOptionPane.showMessageDialog(jPanel, "Fill the numeric parameters");
					else
						JOptionPane.showMessageDialog(jPanel, "Select just one node");
				} catch (Exception e1) {
					JOptionPane.showMessageDialog(jPanel, e1.getStackTrace()[0].getMethodName()+" "+e1.getStackTrace()[0].getLineNumber()+" "+e1.getStackTrace()[0].getClassName()+" "+e1.getMessage()+" "+e1.getClass().toString());
				}

			}

			private double getCost(int lenghtB, int angleB, DefaultWeightedEdge edge) {
				Coord s = graph.getEdgeSource(edge);
				Coord t = graph.getEdgeTarget(edge);
				return (lenghtB*Math.hypot(s.x-t.x,s.y-t.y)/300+angleB*(Math.sin(Math.atan2(t.y-s.y, t.x-s.x))+1)/2)/(lenghtB+angleB);
			}
		});
		jPanel.add(jButton);
	}

}
