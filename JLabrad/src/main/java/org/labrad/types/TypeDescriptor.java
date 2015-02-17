package org.labrad.types;

public class TypeDescriptor {
  private final Type type;
  private final String tag;
  
  /**
   * Create a type descriptor with a customized tag.
   * @param tag
   */
  public TypeDescriptor(String tag) {
    this.type = Type.fromTag(tag);
    this.tag = tag;
  }
  
  /**
   * Create a type descriptor from a type, using the default tag.
   * @param type
   */
  public TypeDescriptor(Type type) {
    this.type = type;
    this.tag = type.toString();
  }
  
  public Type getType() { return type; }
  public String getTag() { return tag; }
}
