/*
 * author: haileiy
 */


public class RetrievalModelIndri extends RetrievalModel {
	
	public double Indri_mu;
    public double Indri_lambda;
    
    /**
     * Set a retrieval model parameter.
     * @param parameterName
     * @param parametervalue
     * @return Always false because this retrieval model has no parameters.
     */
    public boolean setParameter (String parameterName, double value) {
    	if (parameterName.equalsIgnoreCase("mu")) {
    		Indri_mu = value;
    		return true;
    	}
    	else if (parameterName.equalsIgnoreCase("lambda")) {
    		Indri_lambda = value;
    		return true;
    	}
    	else {
	        System.err.println ("Error: Unknown parameter name for retrieval model " +
	                            "Indri: " +
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
