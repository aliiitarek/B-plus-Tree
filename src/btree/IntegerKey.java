package btree;

public class IntegerKey extends KeyClass{

	
	 private int key;
	
	 public IntegerKey(Integer value) {
		 key = value;
	 }
	 
	 
	 public IntegerKey(int value) {
		 key = value;
	 }
	 
	 
	 public Integer getKey() {
		 return key;
	 }
	 
	 
	 public void setKey(Integer value) {
		 key = value;
	 }
	
	 
	 @Override
	public String toString() {
		// TODO Auto-generated method stub
		return key+"";
	}
	 
	 
	 
}
