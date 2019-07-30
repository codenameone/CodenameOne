package com.codename1.location;

public abstract class LatLng {

	 public abstract void setLatitude(double latitude);
	
	 public abstract void setLongitude(double longitude);
	
	 public abstract double getLatitude();
	 
	 public abstract double getLongitude();
	 
	 @Override
	 public int hashCode() {
		 final int prime = 31;
		 int result = 1;
		 long temp;
		 temp = Double.doubleToLongBits(this.getLatitude());
		 result = prime * result + (int) (temp ^ (temp >>> 32));
		 temp = Double.doubleToLongBits(this.getLongitude());
		 result = prime * result + (int) (temp ^ (temp >>> 32));
		 return result;
	 }

	 @Override
	 public boolean equals(Object object) {
		 if (this == object) 
			 return true;
		 if (object == null)
			return false;
		 if (!(object instanceof LatLng)) 
			 return false;
		 LatLng aPosition = (LatLng) object;
		 return (aPosition.getLatitude()==this.getLatitude() && aPosition.getLongitude()==this.getLongitude());
		 //return (Double.doubleToLongBits(aPosition.getLatitude())==Double.doubleToLongBits(this.getLatitude()) && Double.doubleToLongBits(aPosition.getLongitude())==Double.doubleToLongBits(this.getLongitude()));
	 }
}
