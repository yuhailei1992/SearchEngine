public class RetrievalModelBM25 extends RetrievalModel {

	public double BM25_k_1;
    public double BM25_b;
    public double BM25_k_3;
	
    /**
     * Set a retrieval model parameter.
     * @param parameterName
     * @param parametervalue
     * @return Always false because this retrieval model has no parameters.
     */
    public boolean setParameter (String parameterName, double value) {
    	
    	if (parameterName.equalsIgnoreCase("k1")) {
    		BM25_k_1 = value;
    		return true;
    	}
    	else if (parameterName.equalsIgnoreCase("k3")) {
    		BM25_k_3 = value;
    		return true;
    	}
    	else if (parameterName.equalsIgnoreCase("b")) {
    		BM25_b = value;
    		return true;
    	}
    	else {
	        System.err.println ("Error: Unknown parameter name for retrieval model " +
	                            "BM25: " +
	                            parameterName);
	        return false;
    	}
    	
    }

    /**
     * Set a retrieval model parameter.
     * @param parameterName
     * @param parametervalue
     * @return Always false because this retrieval model has no parameters.
     */
    public boolean setParameter (String parameterName, String value) {
        System.err.println ("Error: Unknown parameter name for retrieval model " +
                            "Indri: " +
                            parameterName);
        return false;
    }

}
