package edu.unm.cs.cs361.f12.locd011.subsets;
import java.util.*;     // (Collections framework)

/** An immutable set class
 * Each instance is defined by a backing set, and a size, k.
 * It contains all k-element subsets of the backing set.
 * 
 * @author David Lochridge
 *
 * @param <E> The objects contained in our initial set
 */
public class SizeKSubset<E> extends AbstractSet<Set<E>> {

	 // The AbstractSet class provides defaults for all
	 // methods of the Set interface.  We only need to 
	 // supply the size and iterator methods.
	 
	/**
	 * The set for which this class represents subsets of
	 */
	 Set<E> backingSet;  
	 // We will assume that backingSet is unchanging, at least
	 // for the lifetime of our SizeKSubset object.
	 /**
	  * The size of all of the subsets contained in this set
	  */
	 int k;
	 
	 private int size = -1;
	 
	 /**
	  * An iterator for this set, which returns generated subsets
	  */
	 public Iterator<Set<E>> iterator( ) {
		return new SizeKSubsetIterator<E>( this );
	 }
	 
	 /**
	  * Returns the number of k sized subsets possible
	  */
	 public int size( ) {
		if(size == -1 && (k <= backingSet.size()) && 0 < k){
			size = 1;
			int n = backingSet.size();
			int z = n - k < k ? n-k : k;
			for(int x = 0; x < k; x++){
				if(x < z)
					size *= n - x;
				size /= (x + 1);
			}
		} else if(size == -1)
			size = 0;
		return size;
	 }
	 
	 /**
	  * Initializes a subset on the backing set
	  * @param backingSet The set for which to generate all k-sized subsets
	  * @param k The size of subsets to generate
	  */
	 public SizeKSubset ( Set<E> backingSet, int k ) {
		this.backingSet = backingSet;
		this.k = k;
	 }
	 
	 /**
	  * Generates all possible subsets, and attempts to generate k=0 and k=size+1 sets
	  * @param args A space delimited list of all of the elements in the given set
	  */
	 public static void main ( String[ ] args ) {
		for(int x = 0; x < args.length + 2; x++){
			SizeKSubset<String> s = new SizeKSubset<String>( new TreeSet<String>( Arrays.asList( args ) ), x ) ;
			int counter = 0;
			System.out.println("Subsets of size: " + x);
			long currTime = System.currentTimeMillis();
			for ( Set<String> set : s ) {
				//System.out.println( set );
				counter++;
			}
			System.out.println("Expected subsets: " + s.size());
			System.out.println("Actual   subsets: " + counter);
			System.out.println("Took: " + (System.currentTimeMillis() - currTime) + "ms");
		}
	 }
}