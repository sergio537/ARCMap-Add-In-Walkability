import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import com.esri.arcgis.addins.desktop.Button;
import com.esri.arcgis.framework.IApplication;
import com.esri.arcgis.interop.AutomationException;


public class ParametersButton extends Button {
	
	private enum Weather {
		SUNNY,CLOUDY,RAINY;
	}
	private class CheckTimeModifier implements ActionListener {
		private int i;
		
		public CheckTimeModifier(int i) {
			super();
			this.i = i;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(timesC[i].isSelected())
				sunnysC[i].setEnabled(true);
			else {
				sunnysC[i].setEnabled(false);
				textsS[i].setEnabled(false);
				textsR[i].setEnabled(false);
			}
		}
	}
	
	private class CheckSunnyModifier implements ActionListener {
		private int i;

		
		public CheckSunnyModifier(int i) {
			super();
			this.i = i;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if(sunnysC[i].isSelected()) {
				textsS[i].setEnabled(true);
				textsR[i].setEnabled(true);
			}
			else {
				textsS[i].setEnabled(false);
				textsR[i].setEnabled(false);
			}
		}	
	}

	static double max = 25.0/3;
	static double speed = 3.6*1000/60;
	static double betaT = -0.0193;
	static double lambda = -0.3;
	static double rotationTime = 5.0/60;
	static Weather sunny = Weather.CLOUDY;
	private static double[] betas;
	private static boolean[] betasTime;
	private static boolean[] sunnyBs;
	private static double[] sunnyBetas;
	private static double[] rainyBetas;
	private static boolean changed = true;
	private static boolean nextStep = false;
	public static boolean isEnabled = false;
	private JCheckBox[] timesC;
	private JCheckBox[] sunnysC;
	private JTextField[] textsS;
	private JTextField[] textsR;
	
	@Override
	public void init(IApplication app) throws AutomationException, IOException {
		
	}
	/**
	 * Called when the button is clicked.
	 * 
	 * @exception java.io.IOException if there are interop problems.
	 * @exception com.esri.arcgis.interop.AutomationException if the component throws an ArcObjects exception.
	 */
	@Override
	public void onClick() throws IOException, AutomationException {
		if(isEnabled) {
			try {
				CalculateButton.isEnabled = false;
				CalculateBulkButton.isEnabled = false;
				betas = new double[PrepareButton.fieldsCost.size()];
				betasTime = new boolean[PrepareButton.fieldsCost.size()];
				sunnyBs = new boolean[PrepareButton.fieldsCost.size()];
				sunnyBetas = new double[PrepareButton.fieldsCost.size()];
				rainyBetas = new double[PrepareButton.fieldsCost.size()];
				BufferedReader reader = new BufferedReader(new FileReader(System.getenv("AGSDESKTOPJAVA")+"bin\\Addins\\parameters.txt"));
				reader.readLine();
				Map<String,Double> preBetas = new HashMap<String, Double>();
				Map<String,Boolean> preBetasT = new HashMap<String, Boolean>();
				Map<String,Boolean> preSunnys = new HashMap<String, Boolean>();
				Map<String,Double> preBetasS = new HashMap<String, Double>();
				Map<String,Double> preBetasR = new HashMap<String, Double>();
				String line = reader.readLine();
				while(line!=null) {
					String[] parts = line.split("=");
					String key = parts[0].trim().toLowerCase();
					preBetas.put(key, Double.parseDouble(parts[1].trim()));
					boolean isTime = parts[2].trim().toLowerCase().equals("yes");
					preBetasT.put(key, isTime);
					if(isTime) {
						boolean isSunny = parts[3].trim().toLowerCase().equals("yes");
						preSunnys.put(key, isSunny);
						if(isSunny) {
							preBetasS.put(key, Double.parseDouble(parts[4].trim()));
							preBetasR.put(key, Double.parseDouble(parts[5].trim()));
						}
					}
					line = reader.readLine();
				}
				reader.close();
				nextStep = false;
				final JFrame window = new JFrame("Parameters");
				window.setIconImage(PrepareButton.icon);
				window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				window.setAlwaysOnTop(true);
				window.setLocationRelativeTo(null);
				window.setLayout(new BorderLayout());
				JPanel panel = new JPanel();
				panel.setLayout(new GridLayout(6, 2));
				panel.add(new JLabel("Name"));
				panel.add(new JLabel("Value"));
				panel.add(new JLabel("Maximum distance (m)"));
				final JTextField maxT= new JTextField((int)(max*speed*100)/100.0+"");
				panel.add(maxT);
				panel.add(new JLabel("Walking speed (km/h)"));
				final JTextField speedT= new JTextField((int)(speed*60*100/1000)/100.0+"");
				panel.add(speedT);
				panel.add(new JLabel("b_time"));
				final JTextField betaTT= new JTextField(betaT+"");
				panel.add(betaTT);
				panel.add(new JLabel("Lambda (1/min)"));
				final JTextField lambdaT= new JTextField(lambda+"");
				panel.add(lambdaT);
				/*panel.add(new JLabel("Rotation time cost (s)"));
				final JTextField rotT= new JTextField((int)(rotationTime*60)+"");
				panel.add(rotT);*/
				panel.add(new JLabel("Weather condition"));
				final JComboBox sunnyCombo= new JComboBox(Weather.values());
				sunnyCombo.setSelectedItem(Weather.CLOUDY);
				panel.add(sunnyCombo);
				window.add(panel, BorderLayout.NORTH);
				JPanel panelBetas = new JPanel();
				panelBetas.setLayout(new GridLayout(PrepareButton.fieldsCost.size()+1, 6));
				panelBetas.setBorder(new TitledBorder("Betas"));
				panelBetas.add(new JLabel(" Name "));
				panelBetas.add(new JLabel(" Value "));
				panelBetas.add(new JLabel(" Time dependant "));
				panelBetas.add(new JLabel(" Sunny/Rainy "));
				panelBetas.add(new JLabel(" Value Sunny "));
				panelBetas.add(new JLabel(" Value Rainy "));
				final JTextField[] texts= new JTextField[PrepareButton.fieldsCost.size()];
				timesC= new JCheckBox[PrepareButton.fieldsCost.size()];
				sunnysC= new JCheckBox[PrepareButton.fieldsCost.size()];
				textsS= new JTextField[PrepareButton.fieldsCost.size()];
				textsR= new JTextField[PrepareButton.fieldsCost.size()];
				int i=0;
				for(PrepareButton.FieldCost fieldCost:PrepareButton.fieldsCost.values()) {
					panelBetas.add(new JLabel(fieldCost.name));
					String key = fieldCost.name.trim().toLowerCase();
					Double preBeta = preBetas.get(key);
					texts[i] = new JTextField(preBeta==null?"1.0":preBeta+"");
					panelBetas.add(texts[i]);
					Boolean preBetaT = preBetasT.get(key);
					preBetaT = preBetaT==null?false:preBetaT;
					timesC[i] = new JCheckBox("",preBetaT);
					timesC[i].addActionListener(new CheckTimeModifier(i));
					panelBetas.add(timesC[i]);
					sunnysC[i] = new JCheckBox("");
					sunnysC[i].addActionListener(new CheckSunnyModifier(i));
					panelBetas.add(sunnysC[i]);
					if(preBetaT) {
						Boolean preSunny = preSunnys.get(key);
						preSunny = preSunny==null?false:preSunny;
						sunnysC[i].setSelected(preSunny);
						if(preSunny) {
							Double preBetaS = preBetasS.get(key);
							textsS[i] = new JTextField(preBetaS==null?"1.0":preBetaS+"");
							panelBetas.add(textsS[i]);
							Double preBetaR = preBetasR.get(key);
							textsR[i] = new JTextField(preBetaR==null?"1.0":preBetaR+"");
							panelBetas.add(textsR[i]);
						}
						else {
							textsS[i] = new JTextField("1.0");
							textsS[i].setEnabled(false);
							panelBetas.add(textsS[i]);
							textsR[i] = new JTextField("1.0");
							textsR[i].setEnabled(false);
							panelBetas.add(textsR[i]);
						}
					}
					else {
						sunnysC[i].setSelected(false);
						sunnysC[i].setEnabled(false);
						textsS[i] = new JTextField("1.0");
						textsS[i].setEnabled(false);
						panelBetas.add(textsS[i]);
						textsR[i] = new JTextField("1.0");
						textsR[i].setEnabled(false);
						panelBetas.add(textsR[i]);
					}
					i++;
				}
				window.add(panelBetas,BorderLayout.CENTER);
				JButton jButton = new JButton("Apply");
				jButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						try {
							if(Double.parseDouble(speedT.getText())*1000/60!=speed) {
								speed = Double.parseDouble(speedT.getText())*1000/60;
								changed = true;
							}
							if(Double.parseDouble(maxT.getText())/speed!=max) {
								max = Double.parseDouble(maxT.getText())/speed;
								changed = true;
							}
							if(Double.parseDouble(betaTT.getText())!=betaT) {
								betaT = Double.parseDouble(betaTT.getText());
								changed = true;
							}
							if(Double.parseDouble(lambdaT.getText())!=lambda) {
								lambda = Double.parseDouble(lambdaT.getText());
								changed = true;
							}
							if(sunnyCombo.getSelectedItem()!=sunny) {
								sunny = (Weather) sunnyCombo.getSelectedItem();
								changed = true;
							}
							int i=0;
							for(JTextField text:texts) {
								if(betas[i]!=Double.parseDouble(text.getText())) {
									betas[i] = Double.parseDouble(text.getText());
									changed = true;
								}
								i++;
							}
							i=0;
							for(JCheckBox check:timesC) {
								if(betasTime[i]!=check.isSelected()) {
									betasTime[i] = check.isSelected();
									changed = true;
								}
								i++;
							}
							i=0;
							for(JCheckBox check:sunnysC) {
								if(sunnyBs[i]!=check.isSelected()) {
									sunnyBs[i] = check.isSelected();
									changed = true;
								}
								i++;
							}
							i=0;
							for(JTextField text:textsS) {
								if(sunnyBetas[i]!=Double.parseDouble(text.getText())) {
									sunnyBetas[i] = Double.parseDouble(text.getText());
									changed = true;
								}
								i++;
							}
							i=0;
							for(JTextField text:textsR) {
								if(rainyBetas[i]!=Double.parseDouble(text.getText())) {
									rainyBetas[i] = Double.parseDouble(text.getText());
									changed = true;
								}
								i++;
							}
							nextStep = true;
						} catch(Exception e1) {
							JOptionPane.showMessageDialog(null, e1.getMessage());
						}
					}
				});
				window.add(jButton,BorderLayout.SOUTH);
				window.pack();
				window.setVisible(true);
				while(!nextStep)
					Thread.sleep(100);
				if(changed) {
					for(PrepareButton.EdgeInfo edgeInfo:PrepareButton.edgesInfo) {
						double cost = getCost(edgeInfo.lenght, edgeInfo.values);
						PrepareButton.graph.setEdgeWeight(edgeInfo.edge, cost);
					}
				}
				window.setVisible(false);
				changed = false;
				//Connect nodes according to turning
				
				CalculateButton.isEnabled = true;
				CalculateBulkButton.isEnabled = true;
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
		else
			JOptionPane.showMessageDialog(null, "The scenario has not been prepared");
	}
	
	private static double getCost(double lenght, Double[] values) {
		if(PrepareButton.fieldsCost.size()==0)
			return lenght/speed;
		double costS = 0;
		int i=0;
		for(PrepareButton.FieldCost fieldCost:PrepareButton.fieldsCost.values()) {
			if(!betasTime[i])
				costS -= betas[i]*values[fieldCost.index];
			i++;
		}
		i=0;
		double costM = 1;
		for(PrepareButton.FieldCost fieldCost:PrepareButton.fieldsCost.values()) {
			if(betasTime[i]) {
				double srFactor = 1;
				if(sunnyBs[i])
					srFactor += sunnyBetas[i]*(sunny==Weather.SUNNY?1:0) + rainyBetas[i]*(sunny==Weather.RAINY?1:0);
				costM *= (1+betas[i]*values[fieldCost.index]*srFactor);
			}
			i++;
		}
		double tCost = costS-betaT*lenght*costM/speed;
		/*if(Math.abs(lenght-35.6896)<0.001) {
			String t="";
			for(Double v:values)
				t+=v+",";
			JOptionPane.showMessageDialog(null, t);
			String t2="";
			for(double b:betas)
				t2+=b+",";
			JOptionPane.showMessageDialog(null, t2);
			JOptionPane.showMessageDialog(null, costS+"-"+betaT+"*"+lenght+"*"+costM+"/"+speed+"="+tCost);
			JOptionPane.showMessageDialog(null, ((Double.isNaN(tCost)||Double.isInfinite(tCost))?0:(-tCost/betaT))+"");
		}*/
		return (Double.isNaN(tCost)||Double.isInfinite(tCost))?0:(-tCost/betaT);
	}

}
