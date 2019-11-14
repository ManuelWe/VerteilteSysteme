package eu.boxwork.dhbw.utils;
/*
 * D-Bus Java Implementation Copyright (c) 2005-2006 Matthew Johnson This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of either the GNU Lesser General Public License Version 2 or the
 * Academic Free Licence Version 2.1. Full licence texts are included in the
 * COPYING file with this program.
 */
//package org.freedesktop.dbus;

//import static org.freedesktop.dbus.Gettext._;

import java.math.BigInteger;

/**
 * Class to represent unsigned 64-bit numbers. Warning: Any functions which take
 * or return a <tt>long</tt> are restricted to the range of a signed 64bit
 * number. Use the BigInteger methods if you wish access to the full range.
 */
@SuppressWarnings("serial")
public class UInt64 extends Number implements Comparable<UInt64> {
  /** Maximum allowed value (when accessed as a BigInteger) */
  public static final BigInteger MAX_BIG_VALUE  = new BigInteger("18446744073709551615");
  /** Maximum allowed value (when accessed as a long) */
  public static final long       MAX_LONG_VALUE = Long.MAX_VALUE;
  /** Minimum allowed value */
  public static final long       MIN_VALUE      = 0;
  private long                   bottom;
  private long                   top;
  private BigInteger             value;

  /**
   * Create a UInt64 from a BigInteger
   * 
   * @param value
   *          Must be a valid BigInteger between MIN_VALUE&ndash;MAX_BIG_VALUE
   * @throws NumberFormatException
   *           if value is not an integer between MIN_VALUE and MAX_BIG_VALUE
   */
  public UInt64(BigInteger value) {
/*    if (null == value) { throw new NumberFormatException(MessageFormat.format(_("{0} is not between {1} and {2}."), new Object[] {
        value, MIN_VALUE, MAX_BIG_VALUE })); }
    if (0 > value.compareTo(BigInteger.ZERO)) { throw new NumberFormatException(MessageFormat.format(
        _("{0} is not between {1} and {2}."), new Object[] { value, MIN_VALUE, MAX_BIG_VALUE })); }
    if (0 < value.compareTo(MAX_BIG_VALUE)) { throw new NumberFormatException(MessageFormat.format(
        _("{0} is not between {1} and {2}."), new Object[] { value, MIN_VALUE, MAX_BIG_VALUE })); }
  */  this.value = value;
    top = this.value.shiftRight(32).and(new BigInteger("4294967295")).longValue();
    bottom = this.value.and(new BigInteger("4294967295")).longValue();
  }

  /**
   * Create a UInt64 from a long.
   * 
   * @param value
   *          Must be a valid integer within MIN_VALUE&ndash;MAX_VALUE
   * @throws NumberFormatException
   *           if value is not between MIN_VALUE and MAX_VALUE
   */
  public UInt64(long value) {
    //if ((value < MIN_VALUE) || (value > MAX_LONG_VALUE)) { throw new NumberFormatException(MessageFormat.format(
     //   _("{0} is not between {1} and {2}."), new Object[] { value, MIN_VALUE, MAX_LONG_VALUE })); }
    this.value = new BigInteger("" + value);
    top = this.value.shiftRight(32).and(new BigInteger("4294967295")).longValue();
    bottom = this.value.and(new BigInteger("4294967295")).longValue();
  }

  /**
   * Create a UInt64 from two longs.
   * 
   * @param top
   *          Most significant 4 bytes.
   * @param bottom
   *          Least significant 4 bytes.
   */
  public UInt64(long top, long bottom) {
    BigInteger a = new BigInteger("" + top);
    a = a.shiftLeft(32);
    a = a.add(new BigInteger("" + bottom));
   // if (0 > a.compareTo(BigInteger.ZERO)) { throw new NumberFormatException(MessageFormat.format(
    //    _("{0} is not between {1} and {2}."), new Object[] { a, MIN_VALUE, MAX_BIG_VALUE })); }
    //if (0 < a.compareTo(MAX_BIG_VALUE)) { throw new NumberFormatException(MessageFormat.format(
     //   _("{0} is not between {1} and {2}."), new Object[] { a, MIN_VALUE, MAX_BIG_VALUE })); }
    value = a;
    this.top = top;
    this.bottom = bottom;
  }

  /**
   * Create a UInt64 from a String.
   * 
   * @param value
   *          Must parse to a valid integer within MIN_VALUE&ndash;MAX_BIG_VALUE
   * @throws NumberFormatException
   *           if value is not an integer between MIN_VALUE and MAX_BIG_VALUE
   */
  public UInt64(String value) {
    //if (null == value) { throw new NumberFormatException(MessageFormat.format(_("{0} is not between {1} and {2}."), new Object[] {
      //  value, MIN_VALUE, MAX_BIG_VALUE })); }
    BigInteger a = new BigInteger(value);
    this.value = a;
    top = this.value.shiftRight(32).and(new BigInteger("4294967295")).longValue();
    bottom = this.value.and(new BigInteger("4294967295")).longValue();
  }

  /**
   * Least significant 4 bytes.
   */
  public long bottom() {
    return bottom;
  }

  /** The value of this as a byte. */
  @Override
  public byte byteValue() {
    return value.byteValue();
  }

  /**
   * Compare two UInt32s.
   * 
   * @return 0 if equal, -ve or +ve if they are different.
   */
  public int compareTo(UInt64 other) {
    return value.compareTo(other.value);
  }

  /** The value of this as a double. */
  @Override
  public double doubleValue() {
    return value.doubleValue();
  }

  /** Test two UInt64s for equality. */
  @Override
  public boolean equals(Object o) {
    return (o instanceof UInt64) && value.equals(((UInt64) o).value);
  }

  /** The value of this as a float. */
  @Override
  public float floatValue() {
    return value.floatValue();
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  /** The value of this as a int. */
  @Override
  public int intValue() {
    return value.intValue();
  }

  /** The value of this as a long. */
  @Override
  public long longValue() {
    return value.longValue();
  }

  /** The value of this as a short. */
  @Override
  public short shortValue() {
    return value.shortValue();
  }

  /**
   * Most significant 4 bytes.
   */
  public long top() {
    return top;
  }

  /** The value of this as a string. */
  @Override
  public String toString() {
    return value.toString();
  }

  /** The value of this as a BigInteger. */
  public BigInteger value() {
    return value;
  }
  
  /**
   * add both input values and returns a new UInt64
   * */
  public static UInt64 plus(UInt64 a, UInt64 b)
  {
	  long tA = a.top();
	  
	  // check overrun bottom
	  long newBottom = 0l;
	  long overrunAdd = 0l;
	  if ((MAX_LONG_VALUE-a.bottom())<=(MAX_LONG_VALUE-b.bottom()))
	  {
		  // no overrun, just add buttom
		  newBottom = a.bottom() + b.bottom();
	  }
	  else
	  {
		  // overrun
		  overrunAdd = 1l;
		  newBottom = MAX_LONG_VALUE - a.bottom() + b.bottom();
	  }
	  
	  // not the high value
	  long newHigh = 0l;
	  if ((MAX_LONG_VALUE - a.top() - overrunAdd)<=(MAX_LONG_VALUE - b.top()))
	  {
		  // no overrun, just add high value and overrun
		  newHigh = a.top() + b.top()+overrunAdd;
	  }
	  else
	  {
		  // overrun, runtime exception, since no start at zero defined !!!
		  throw new RuntimeException("UInt64 number exception, maxiumum value exceeded.");
	  }
	  return new UInt64(newHigh,newBottom);
  }
}
