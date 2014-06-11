package au.org.intersect.faims.android.util;

public class InputBuffer {
	
	public interface InputBufferListener {
		public void onInput(String inputSequence);
	}

	private InputBufferListener listener;
	private Character endOfInput;
	private StringBuffer buffer;
	
	public InputBuffer(Character endOfInput) {
		this.endOfInput = endOfInput;
		clearBuffer();
	}
	
	public void setListener(InputBufferListener listener) {
		this.listener = listener;
	}
	
	public void addInput(Character c) {
		if (c.equals(endOfInput)) {
			if (listener != null) {
				listener.onInput(buffer.toString());
			}
			clearBuffer();
		} else {
			buffer.append(c);
		}
	}
	
	private void clearBuffer() {
		buffer = new StringBuffer();
	}

}
