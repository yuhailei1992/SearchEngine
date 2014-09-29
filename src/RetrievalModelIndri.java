/*
 * author: haileiy
 */


public class RetrievalModelIndri extends RetrievalModel {

    /**
     * Set a retrieval model parameter.
     * @param parameterName
     * @param parametervalue
     * @return Always false because this retrieval model has no parameters.
     */
    public boolean setParameter (String parameterName, double value) {
        System.err.println ("Error: Unknown parameter name for retrieval model " +
                            "Indri: " +
                            parameterName);
        return false;
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