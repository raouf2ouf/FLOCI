package com.handi.floci.modules.individualclassification;

public class Parser {
	private double a, b, c, d = 0;
	private int i;
	private String type;
	public Parser(String s) {
		try {
			i = s.indexOf(" type") + 5;
			
			while(s.charAt(i) != '"') { i++; }
			i++;
			type = "";
			while(s.charAt(i) != '\\') {
				type += s.charAt(i);
				i++;
			}
			// à ce stade on a le type 
			a = readParameter(s, 'a'); // à ce stade on a le A
			b = readParameter(s, 'b'); // à ce stade on a le B
			if(type.equals("triangular")) {
				c = readParameter(s, 'c'); // à ce stade on a le C
				
			} else if(type.equals("trapezoidal")) {
				c = readParameter(s, 'c'); // à ce stade on a le C
				d = readParameter(s, 'd'); // à ce stade on a le D
			}
			
		} catch(Exception e) {
			System.out.println("Erreur lors du parsssage du label fuzzy");
		}
	}
	
	
	private double readParameter(String s, char c) {
		while(s.charAt(i) != c) { i++;}
		while(s.charAt(i) != '"') { i++; }
		i++;
		String cString = "";
		while(s.charAt(i) != '\\') {
			cString += s.charAt(i);
			i++;
		}
		return Double.parseDouble(cString);
	}
	
	
	public double getDegree(double x) {
		try {
			if(type.equals("leftshoulder")) {
				return round(leftShoulder(x, a, b));
			}
			if(type.equals("rightshoulder")) {
				return round(rightShoulder(x, a, b));
			}
			if(type.equals("triangular")) {
				return round(triangular(x, a, b, c));
			}
			if(type.equals("trapezoidal")) {
				return round(trapezoidal(x, a, b, c, d));
			}
			return 0;
		} catch(Exception e) {
			return 0;
		}
	}
	
	public double leftShoulder(double x, double a, double b) {
		if(x <= a) return 1;
		if(x >= b) return 0;
		double resultat = (b-x)/(b-a);
		return resultat;
	}
	
	public double rightShoulder(double x, double a, double b) {
		if(x <= a) return 0;
		if(x >= b) return 1;
		double resultat = (x-a)/(b-a);
		return resultat;		
	}
	
	public double triangular(double x, double a, double b, double c) {
		if(x <= a) return 0;
		if(x >= c) return 0;
		if(x > a && x < b) return (x-a)/(b-a);
		return (c-x)/(c-b);
	}
	
	public double trapezoidal(double x, double a, double b, double c, double d) {
		if(x <= a) return 0;
		if(x >= d) return 0;
		if(x > a && x < b) return (x-a)/(b-a);
		if(x >b && x < c) return 1;
		return (d-x)/(d-c);
	}
	
	private double round(double val) {
		val = val*100;
		val = Math.round(val);
		val = val /100;
		return val;
	}
}
