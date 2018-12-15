


package org.platon.storage.trie;

public final class TrieProto {
  private TrieProto() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  public interface NodeBaseOrBuilder extends

      com.google.protobuf.MessageOrBuilder {

    
    com.google.protobuf.ByteString getHash();

    
    com.google.protobuf.ByteString getKey();

    
    java.util.List<com.google.protobuf.ByteString> getValueOrNodeHashList();
    
    int getValueOrNodeHashCount();
    
    com.google.protobuf.ByteString getValueOrNodeHash(int index);

    
    boolean hasChildBase();
    
    NodeBase getChildBase();
    
    NodeBaseOrBuilder getChildBaseOrBuilder();

    
    com.google.protobuf.ByteString getChildEncode();

    
    int getChildBasePos();
  }
  
  public  static final class NodeBase extends
      com.google.protobuf.GeneratedMessageV3 implements

      NodeBaseOrBuilder {
  private static final long serialVersionUID = 0L;

    private NodeBase(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
      super(builder);
    }
    private NodeBase() {
      hash_ = com.google.protobuf.ByteString.EMPTY;
      key_ = com.google.protobuf.ByteString.EMPTY;
      valueOrNodeHash_ = java.util.Collections.emptyList();
      childEncode_ = com.google.protobuf.ByteString.EMPTY;
      childBasePos_ = 0;
    }

    @Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return this.unknownFields;
    }
    private NodeBase(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      if (extensionRegistry == null) {
        throw new NullPointerException();
      }
      int mutable_bitField0_ = 0;
      com.google.protobuf.UnknownFieldSet.Builder unknownFields =
          com.google.protobuf.UnknownFieldSet.newBuilder();
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!parseUnknownFieldProto3(
                  input, unknownFields, extensionRegistry, tag)) {
                done = true;
              }
              break;
            }
            case 10: {

              hash_ = input.readBytes();
              break;
            }
            case 18: {

              key_ = input.readBytes();
              break;
            }
            case 26: {
              if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
                valueOrNodeHash_ = new java.util.ArrayList<com.google.protobuf.ByteString>();
                mutable_bitField0_ |= 0x00000004;
              }
              valueOrNodeHash_.add(input.readBytes());
              break;
            }
            case 34: {
              Builder subBuilder = null;
              if (childBase_ != null) {
                subBuilder = childBase_.toBuilder();
              }
              childBase_ = input.readMessage(NodeBase.parser(), extensionRegistry);
              if (subBuilder != null) {
                subBuilder.mergeFrom(childBase_);
                childBase_ = subBuilder.buildPartial();
              }

              break;
            }
            case 42: {

              childEncode_ = input.readBytes();
              break;
            }
            case 48: {

              childBasePos_ = input.readInt32();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
          valueOrNodeHash_ = java.util.Collections.unmodifiableList(valueOrNodeHash_);
        }
        this.unknownFields = unknownFields.build();
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return TrieProto.internal_static_NodeBase_descriptor;
    }

    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return TrieProto.internal_static_NodeBase_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              NodeBase.class, Builder.class);
    }

    private int bitField0_;
    public static final int HASH_FIELD_NUMBER = 1;
    private com.google.protobuf.ByteString hash_;
    
    public com.google.protobuf.ByteString getHash() {
      return hash_;
    }

    public static final int KEY_FIELD_NUMBER = 2;
    private com.google.protobuf.ByteString key_;
    
    public com.google.protobuf.ByteString getKey() {
      return key_;
    }

    public static final int VALUEORNODEHASH_FIELD_NUMBER = 3;
    private java.util.List<com.google.protobuf.ByteString> valueOrNodeHash_;
    
    public java.util.List<com.google.protobuf.ByteString>
        getValueOrNodeHashList() {
      return valueOrNodeHash_;
    }
    
    public int getValueOrNodeHashCount() {
      return valueOrNodeHash_.size();
    }
    
    public com.google.protobuf.ByteString getValueOrNodeHash(int index) {
      return valueOrNodeHash_.get(index);
    }

    public static final int CHILDBASE_FIELD_NUMBER = 4;
    private NodeBase childBase_;
    
    public boolean hasChildBase() {
      return childBase_ != null;
    }
    
    public NodeBase getChildBase() {
      return childBase_ == null ? NodeBase.getDefaultInstance() : childBase_;
    }
    
    public NodeBaseOrBuilder getChildBaseOrBuilder() {
      return getChildBase();
    }

    public static final int CHILDENCODE_FIELD_NUMBER = 5;
    private com.google.protobuf.ByteString childEncode_;
    
    public com.google.protobuf.ByteString getChildEncode() {
      return childEncode_;
    }

    public static final int CHILDBASEPOS_FIELD_NUMBER = 6;
    private int childBasePos_;
    
    public int getChildBasePos() {
      return childBasePos_;
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (!hash_.isEmpty()) {
        output.writeBytes(1, hash_);
      }
      if (!key_.isEmpty()) {
        output.writeBytes(2, key_);
      }
      for (int i = 0; i < valueOrNodeHash_.size(); i++) {
        output.writeBytes(3, valueOrNodeHash_.get(i));
      }
      if (childBase_ != null) {
        output.writeMessage(4, getChildBase());
      }
      if (!childEncode_.isEmpty()) {
        output.writeBytes(5, childEncode_);
      }
      if (childBasePos_ != 0) {
        output.writeInt32(6, childBasePos_);
      }
      unknownFields.writeTo(output);
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (!hash_.isEmpty()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(1, hash_);
      }
      if (!key_.isEmpty()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(2, key_);
      }
      {
        int dataSize = 0;
        for (int i = 0; i < valueOrNodeHash_.size(); i++) {
          dataSize += com.google.protobuf.CodedOutputStream
            .computeBytesSizeNoTag(valueOrNodeHash_.get(i));
        }
        size += dataSize;
        size += 1 * getValueOrNodeHashList().size();
      }
      if (childBase_ != null) {
        size += com.google.protobuf.CodedOutputStream
          .computeMessageSize(4, getChildBase());
      }
      if (!childEncode_.isEmpty()) {
        size += com.google.protobuf.CodedOutputStream
          .computeBytesSize(5, childEncode_);
      }
      if (childBasePos_ != 0) {
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(6, childBasePos_);
      }
      size += unknownFields.getSerializedSize();
      memoizedSize = size;
      return size;
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == this) {
       return true;
      }
      if (!(obj instanceof NodeBase)) {
        return super.equals(obj);
      }
      NodeBase other = (NodeBase) obj;

      boolean result = true;
      result = result && getHash()
          .equals(other.getHash());
      result = result && getKey()
          .equals(other.getKey());
      result = result && getValueOrNodeHashList()
          .equals(other.getValueOrNodeHashList());
      result = result && (hasChildBase() == other.hasChildBase());
      if (hasChildBase()) {
        result = result && getChildBase()
            .equals(other.getChildBase());
      }
      result = result && getChildEncode()
          .equals(other.getChildEncode());
      result = result && (getChildBasePos()
          == other.getChildBasePos());
      result = result && unknownFields.equals(other.unknownFields);
      return result;
    }

    @Override
    public int hashCode() {
      if (memoizedHashCode != 0) {
        return memoizedHashCode;
      }
      int hash = 41;
      hash = (19 * hash) + getDescriptor().hashCode();
      hash = (37 * hash) + HASH_FIELD_NUMBER;
      hash = (53 * hash) + getHash().hashCode();
      hash = (37 * hash) + KEY_FIELD_NUMBER;
      hash = (53 * hash) + getKey().hashCode();
      if (getValueOrNodeHashCount() > 0) {
        hash = (37 * hash) + VALUEORNODEHASH_FIELD_NUMBER;
        hash = (53 * hash) + getValueOrNodeHashList().hashCode();
      }
      if (hasChildBase()) {
        hash = (37 * hash) + CHILDBASE_FIELD_NUMBER;
        hash = (53 * hash) + getChildBase().hashCode();
      }
      hash = (37 * hash) + CHILDENCODE_FIELD_NUMBER;
      hash = (53 * hash) + getChildEncode().hashCode();
      hash = (37 * hash) + CHILDBASEPOS_FIELD_NUMBER;
      hash = (53 * hash) + getChildBasePos();
      hash = (29 * hash) + unknownFields.hashCode();
      memoizedHashCode = hash;
      return hash;
    }

    public static NodeBase parseFrom(
        java.nio.ByteBuffer data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NodeBase parseFrom(
        java.nio.ByteBuffer data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NodeBase parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NodeBase parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NodeBase parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static NodeBase parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static NodeBase parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static NodeBase parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static NodeBase parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static NodeBase parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static NodeBase parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input);
    }
    public static NodeBase parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageV3
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(NodeBase prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @Override
    protected Builder newBuilderForType(
        BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    
    public static final class Builder extends
        com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements

        NodeBaseOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return TrieProto.internal_static_NodeBase_descriptor;
      }

      protected FieldAccessorTable
          internalGetFieldAccessorTable() {
        return TrieProto.internal_static_NodeBase_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                NodeBase.class, Builder.class);
      }


      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessageV3
                .alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        hash_ = com.google.protobuf.ByteString.EMPTY;

        key_ = com.google.protobuf.ByteString.EMPTY;

        valueOrNodeHash_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
        if (childBaseBuilder_ == null) {
          childBase_ = null;
        } else {
          childBase_ = null;
          childBaseBuilder_ = null;
        }
        childEncode_ = com.google.protobuf.ByteString.EMPTY;

        childBasePos_ = 0;

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return TrieProto.internal_static_NodeBase_descriptor;
      }

      public NodeBase getDefaultInstanceForType() {
        return NodeBase.getDefaultInstance();
      }

      public NodeBase build() {
        NodeBase result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public NodeBase buildPartial() {
        NodeBase result = new NodeBase(this);
        int from_bitField0_ = bitField0_;
        int to_bitField0_ = 0;
        result.hash_ = hash_;
        result.key_ = key_;
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          valueOrNodeHash_ = java.util.Collections.unmodifiableList(valueOrNodeHash_);
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.valueOrNodeHash_ = valueOrNodeHash_;
        if (childBaseBuilder_ == null) {
          result.childBase_ = childBase_;
        } else {
          result.childBase_ = childBaseBuilder_.build();
        }
        result.childEncode_ = childEncode_;
        result.childBasePos_ = childBasePos_;
        result.bitField0_ = to_bitField0_;
        onBuilt();
        return result;
      }

      public Builder clone() {
        return (Builder) super.clone();
      }
      public Builder setField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.setField(field, value);
      }
      public Builder clearField(
          com.google.protobuf.Descriptors.FieldDescriptor field) {
        return (Builder) super.clearField(field);
      }
      public Builder clearOneof(
          com.google.protobuf.Descriptors.OneofDescriptor oneof) {
        return (Builder) super.clearOneof(oneof);
      }
      public Builder setRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          int index, Object value) {
        return (Builder) super.setRepeatedField(field, index, value);
      }
      public Builder addRepeatedField(
          com.google.protobuf.Descriptors.FieldDescriptor field,
          Object value) {
        return (Builder) super.addRepeatedField(field, value);
      }
      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof NodeBase) {
          return mergeFrom((NodeBase)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(NodeBase other) {
        if (other == NodeBase.getDefaultInstance()) return this;
        if (other.getHash() != com.google.protobuf.ByteString.EMPTY) {
          setHash(other.getHash());
        }
        if (other.getKey() != com.google.protobuf.ByteString.EMPTY) {
          setKey(other.getKey());
        }
        if (!other.valueOrNodeHash_.isEmpty()) {
          if (valueOrNodeHash_.isEmpty()) {
            valueOrNodeHash_ = other.valueOrNodeHash_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureValueOrNodeHashIsMutable();
            valueOrNodeHash_.addAll(other.valueOrNodeHash_);
          }
          onChanged();
        }
        if (other.hasChildBase()) {
          mergeChildBase(other.getChildBase());
        }
        if (other.getChildEncode() != com.google.protobuf.ByteString.EMPTY) {
          setChildEncode(other.getChildEncode());
        }
        if (other.getChildBasePos() != 0) {
          setChildBasePos(other.getChildBasePos());
        }
        this.mergeUnknownFields(other.unknownFields);
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        NodeBase parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (NodeBase) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }
      private int bitField0_;

      private com.google.protobuf.ByteString hash_ = com.google.protobuf.ByteString.EMPTY;
      
      public com.google.protobuf.ByteString getHash() {
        return hash_;
      }
      
      public Builder setHash(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        hash_ = value;
        onChanged();
        return this;
      }
      
      public Builder clearHash() {
        
        hash_ = getDefaultInstance().getHash();
        onChanged();
        return this;
      }

      private com.google.protobuf.ByteString key_ = com.google.protobuf.ByteString.EMPTY;
      
      public com.google.protobuf.ByteString getKey() {
        return key_;
      }
      
      public Builder setKey(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        key_ = value;
        onChanged();
        return this;
      }
      
      public Builder clearKey() {
        
        key_ = getDefaultInstance().getKey();
        onChanged();
        return this;
      }

      private java.util.List<com.google.protobuf.ByteString> valueOrNodeHash_ = java.util.Collections.emptyList();
      private void ensureValueOrNodeHashIsMutable() {
        if (!((bitField0_ & 0x00000004) == 0x00000004)) {
          valueOrNodeHash_ = new java.util.ArrayList<com.google.protobuf.ByteString>(valueOrNodeHash_);
          bitField0_ |= 0x00000004;
         }
      }
      
      public java.util.List<com.google.protobuf.ByteString>
          getValueOrNodeHashList() {
        return java.util.Collections.unmodifiableList(valueOrNodeHash_);
      }
      
      public int getValueOrNodeHashCount() {
        return valueOrNodeHash_.size();
      }
      
      public com.google.protobuf.ByteString getValueOrNodeHash(int index) {
        return valueOrNodeHash_.get(index);
      }
      
      public Builder setValueOrNodeHash(
          int index, com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureValueOrNodeHashIsMutable();
        valueOrNodeHash_.set(index, value);
        onChanged();
        return this;
      }
      
      public Builder addValueOrNodeHash(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  ensureValueOrNodeHashIsMutable();
        valueOrNodeHash_.add(value);
        onChanged();
        return this;
      }
      
      public Builder addAllValueOrNodeHash(
          Iterable<? extends com.google.protobuf.ByteString> values) {
        ensureValueOrNodeHashIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(
            values, valueOrNodeHash_);
        onChanged();
        return this;
      }
      
      public Builder clearValueOrNodeHash() {
        valueOrNodeHash_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
        return this;
      }

      private NodeBase childBase_ = null;
      private com.google.protobuf.SingleFieldBuilderV3<
          NodeBase, Builder, NodeBaseOrBuilder> childBaseBuilder_;
      
      public boolean hasChildBase() {
        return childBaseBuilder_ != null || childBase_ != null;
      }
      
      public NodeBase getChildBase() {
        if (childBaseBuilder_ == null) {
          return childBase_ == null ? NodeBase.getDefaultInstance() : childBase_;
        } else {
          return childBaseBuilder_.getMessage();
        }
      }
      
      public Builder setChildBase(NodeBase value) {
        if (childBaseBuilder_ == null) {
          if (value == null) {
            throw new NullPointerException();
          }
          childBase_ = value;
          onChanged();
        } else {
          childBaseBuilder_.setMessage(value);
        }

        return this;
      }
      
      public Builder setChildBase(
          Builder builderForValue) {
        if (childBaseBuilder_ == null) {
          childBase_ = builderForValue.build();
          onChanged();
        } else {
          childBaseBuilder_.setMessage(builderForValue.build());
        }

        return this;
      }
      
      public Builder mergeChildBase(NodeBase value) {
        if (childBaseBuilder_ == null) {
          if (childBase_ != null) {
            childBase_ =
              NodeBase.newBuilder(childBase_).mergeFrom(value).buildPartial();
          } else {
            childBase_ = value;
          }
          onChanged();
        } else {
          childBaseBuilder_.mergeFrom(value);
        }

        return this;
      }
      
      public Builder clearChildBase() {
        if (childBaseBuilder_ == null) {
          childBase_ = null;
          onChanged();
        } else {
          childBase_ = null;
          childBaseBuilder_ = null;
        }

        return this;
      }
      
      public Builder getChildBaseBuilder() {
        
        onChanged();
        return getChildBaseFieldBuilder().getBuilder();
      }
      
      public NodeBaseOrBuilder getChildBaseOrBuilder() {
        if (childBaseBuilder_ != null) {
          return childBaseBuilder_.getMessageOrBuilder();
        } else {
          return childBase_ == null ?
              NodeBase.getDefaultInstance() : childBase_;
        }
      }
      
      private com.google.protobuf.SingleFieldBuilderV3<
          NodeBase, Builder, NodeBaseOrBuilder>
          getChildBaseFieldBuilder() {
        if (childBaseBuilder_ == null) {
          childBaseBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
              NodeBase, Builder, NodeBaseOrBuilder>(
                  getChildBase(),
                  getParentForChildren(),
                  isClean());
          childBase_ = null;
        }
        return childBaseBuilder_;
      }

      private com.google.protobuf.ByteString childEncode_ = com.google.protobuf.ByteString.EMPTY;
      
      public com.google.protobuf.ByteString getChildEncode() {
        return childEncode_;
      }
      
      public Builder setChildEncode(com.google.protobuf.ByteString value) {
        if (value == null) {
    throw new NullPointerException();
  }
  
        childEncode_ = value;
        onChanged();
        return this;
      }
      
      public Builder clearChildEncode() {
        
        childEncode_ = getDefaultInstance().getChildEncode();
        onChanged();
        return this;
      }

      private int childBasePos_ ;
      
      public int getChildBasePos() {
        return childBasePos_;
      }
      
      public Builder setChildBasePos(int value) {
        
        childBasePos_ = value;
        onChanged();
        return this;
      }
      
      public Builder clearChildBasePos() {
        
        childBasePos_ = 0;
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.setUnknownFieldsProto3(unknownFields);
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return super.mergeUnknownFields(unknownFields);
      }



    }


    private static final NodeBase DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new NodeBase();
    }

    public static NodeBase getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<NodeBase>
        PARSER = new com.google.protobuf.AbstractParser<NodeBase>() {
      public NodeBase parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
        return new NodeBase(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<NodeBase> parser() {
      return PARSER;
    }

    @Override
    public com.google.protobuf.Parser<NodeBase> getParserForType() {
      return PARSER;
    }

    public NodeBase getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_NodeBase_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_NodeBase_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\021NodeList.v7.proto\"\207\001\n\010NodeBase\022\014\n\004hash" +
      "\030\001 \001(\014\022\013\n\003key\030\002 \001(\014\022\027\n\017valueOrNodeHash\030\003" +
      " \003(\014\022\034\n\tchildBase\030\004 \001(\0132\t.NodeBase\022\023\n\013ch" +
      "ildEncode\030\005 \001(\014\022\024\n\014childBasePos\030\006 \001(\005B-\n" +
      " com.juzix.platon.blockchain.trieB\tTrieP" +
      "rotob\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_NodeBase_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_NodeBase_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_NodeBase_descriptor,
        new String[] { "Hash", "Key", "ValueOrNodeHash", "ChildBase", "ChildEncode", "ChildBasePos", });
  }


}
