package convex.core.crypto;

import java.nio.ByteBuffer;

import convex.core.data.ACell;
import convex.core.data.Address;
import convex.core.exceptions.BadFormatException;
import convex.core.util.Utils;

public abstract class ASignature extends ACell {

	/**
	 * Checks if the signature is valid for a given message hash
	 * @param hash
	 * @param address
	 * @return True if signature is valid, false otherwise
	 */
	public abstract boolean verify(Hash hash, Address address);
	
	/**
	 * Reads a Signature from the given ByteBuffer. Assumes tag byte already read.
	 * 
	 * Uses Ed25519 or ECDSA as configured.
	 * 
	 * @param bb
	 * @throws BadFormatException
	 */
	public static ASignature read(ByteBuffer bb) throws BadFormatException {
		return Ed25519Signature.read(bb);
	}
	
	/**
	 * Gets the content of this Signature as a hex string
	 * @return
	 */
	public abstract String toHexString();
	
	/**
	 * Reads a Signature from the given ByteBuffer. Assumes tag byte already read.
	 * 
	 * Uses Ed25519 or ECDSA as configured.
	 * 
	 * @param bb
	 * @throws BadFormatException
	 */
	public static ASignature fromHex(String hex) throws BadFormatException {
		byte[] bs=Utils.hexToBytes(hex);
		return Ed25519Signature.wrap(bs);
	}
	
	@Override
	public boolean isEmbedded() {
		return true;
	}

}
