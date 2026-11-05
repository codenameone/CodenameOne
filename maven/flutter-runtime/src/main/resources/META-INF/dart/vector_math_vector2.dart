// package:vector_math's 2-component vector and its angle helpers. Mutating
// members change the vector in place, as vector_math's do.
@JavaName('com.codename1.flutter.vectormath.Vector2')
class Vector2 {
  external Vector2(double x, double y);
  external static Vector2 zero();
  external static Vector2 all(double value);
  external static Vector2 copy(Vector2 other);
  external double get x;
  external double get y;
  external set x(double v);
  external set y(double v);
  external double get length;
  external set length(double value);
  external double get length2;
  external void setValues(double x, double y);
  external void setZero();
  external void setFrom(Vector2 other);
  external Vector2 clone();
  external void add(Vector2 arg);
  external void sub(Vector2 arg);
  external void multiply(Vector2 arg);
  external void divide(Vector2 arg);
  external void scale(double arg);
  external void negate();
  external Vector2 scaled(double arg);
  external double normalize();
  external Vector2 normalized();
  external double dot(Vector2 other);
  external double distanceTo(Vector2 arg);
  external Vector2 operator +(Vector2 other);
  external Vector2 operator -(Vector2 other);
  external Vector2 operator *(double scale);
}

@JavaName('com.codename1.flutter.vectormath.Vector2.radians')
external double radians(double degrees);
@JavaName('com.codename1.flutter.vectormath.Vector2.degrees')
external double degrees(double radians);
