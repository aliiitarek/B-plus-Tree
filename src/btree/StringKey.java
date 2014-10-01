package btree;

public class StringKey extends KeyClass{

	
	String key;
	
	public StringKey(String string) {
		key =  string;
	}
	
	
	 public String getKey() {//does this cause a problem ? shall I return a copy ?
		 return key;
	 }
	
	 
	 public void setKey(String s) {
		 key = s;
	 }
	 
	@Override
	public String toString() {
		
		return key;
	}

}
