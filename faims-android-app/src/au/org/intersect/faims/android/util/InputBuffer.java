package au.org.intersect.faims.android.util;

public class InputBuffer {
	
	public interface InputBufferListener {
		public void onFull(String inputSequence);
		public void onInput(String inputSequence);
	}

	private InputBufferListener listener;
	private int bufferSize;
	private Character endOfInput;
	private StringBuffer buffer;
	
	public InputBuffer(int bufferSize, Character endOfInput) {
		this.bufferSize = bufferSize;
		this.endOfInput = endOfInput;
		clearBuffer();
	}
	
	public void setListener(InputBufferListener listener) {
		this.listener = listener;
	}
	
	public void addInput(Character c) {
		if (bufferSize == 0) return;
		
		if (c.equals(endOfInput)) {
			if (listener != null) {
				listener.onInput(bufferToString());
			}
			clearBuffer();
		} else {
			buffer.append(c);
			if (buffer.length() == bufferSize) {
				if (listener != null) {
					listener.onFull(bufferToString());
				}
				clearBuffer();
			}
		}
	}
	
	private String bufferToString() {
		return null;
	}
	
	private void clearBuffer() {
		buffer = new StringBuffer();
	}

}
