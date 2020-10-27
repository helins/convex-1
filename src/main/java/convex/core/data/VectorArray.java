package convex.core.data;

import java.util.ListIterator;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import convex.core.exceptions.InvalidDataException;

/**
 * Experimental: implementation of AVector backed by a Java array for temporary usage purposes.
 *
 * @param <T>
 */
public class VectorArray<T> extends ASizedVector<T> {

	private final T[] array;
	private final int offset;
	private final int stride;

	protected VectorArray(long count, T[] array, int offset, int stride) {
		super(count);
		this.array=array;
		this.offset=offset;
		this.stride=stride;
	}
	
	@Override
	public ListIterator<T> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int estimatedEncodingSize() {
		return 100;
	}

	@Override
	public T get(long i) {
		if ((i < 0) || (i >= count)) throw new IndexOutOfBoundsException("Index: " + i);
		return array[offset+(int)(i*stride)];
	}

	@Override
	public AVector<T> appendChunk(VectorLeaf<T> chunkVector) {
		return toVector().appendChunk(chunkVector);
	}

	@Override
	public VectorLeaf<T> getChunk(long offset) {
		return toVector().getChunk(offset);
	}

	@Override
	public AVector<T> append(T value) {
		return toVector().append(value);
	}

	@Override
	public boolean isPacked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected <R> void copyToArray(R[] arr, int offset) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean anyMatch(Predicate<? super T> pred) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean allMatch(Predicate<? super T> pred) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <R> AVector<R> map(Function<? super T, ? extends R> mapper) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AVector<T> concat(ASequence<T> b) {
		return toVector().concat(b);
	}

	@Override
	public <R> R reduce(BiFunction<? super R, ? super T, ? extends R> func, R value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Spliterator<T> spliterator(long position) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<T> listIterator(long index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCanonical() {
		// Not a canonical vector!
		return false;
	}

	@Override
	public AVector<T> updateRefs(IRefFunction func) {
		return toVector().updateRefs(func);
	}

	@Override
	public long commonPrefixLength(AVector<T> b) {
		return toVector().commonPrefixLength(b);
	}

	@Override
	public AVector<T> next() {
		if (count==0) return null;
		return slice(1,count-1);
	}

	@Override
	public AVector<T> assoc(long i, T value) {
		return toVector().assoc(i,value);
	}

	@Override
	public long longIndexOf(Object value) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long longLastIndexOf(Object value) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void forEach(Consumer<? super T> action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitElementRefs(Consumer<Ref<T>> f) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Ref<T> getElementRef(long index) {
		return Ref.get(get(index));
	}

	@Override
	public VectorArray<T> subVector(long start, long length) {
		checkRange(start, length);
		if (length == count) return this;

		return new VectorArray<T>(length,array,offset+(int)(start*stride),stride);
	}

	@Override
	public int write(byte[] bs, int pos) {
		return toVector().write(bs, pos);
	}

	@SuppressWarnings("unchecked")
	@Override
	public AVector<T> toVector() {
		if (stride==1) Vectors.create(array, offset, (int)count);
		return (AVector<T>) Vectors.create(toArray());
	}

	@Override
	public void validateCell() throws InvalidDataException {
		throw new UnsupportedOperationException();
	}

	@Override
	public int writeRaw(byte[] bs, int pos) {
		return toVector().writeRaw(bs, pos);
	}

	@Override
	public int getRefCount() {
		throw new UnsupportedOperationException();
	}

}