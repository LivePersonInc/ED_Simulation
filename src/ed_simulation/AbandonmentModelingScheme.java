package ed_simulation;

/**
 * Schemes are as follows:
 * EXPONENTIAL_SILENT_CONSIDERED_SERVED - Use exponential patience model only for known abandonment.
 * Silent abandoned (or rather, single exchange conversations) are in fact ignored, and their portion is
 * determined by the geometric distribution determining the number of exchanges per conversations
 * EXPONENTIAL_SILENT_MARKED - Use a single exponential patience model for both known and silent (in fact, single exchange conversations), and mark
 * single exchange conversations before allowing them into service. Single exchange out of all abandoned are determined by the overall ratio between
 * single exchange to known abandoned.
 * SINGLE_EXCHANGE_BASED_ON_HISTOGRAM - Use exponential model for the known abandoned, and external function (histogram based on hadoop data) for the
 * single exchange probability as a function of the wait time.
 * STATE_DEPENDENT_SERVICE_RATE - basically the state is the time in queue (should be TTFR, though!). Per each state, specify a different service rate (service time is
 * still assumed exponential). We can work with two states - abandoned/not abandoned, or multiple ones, in which each ttfr has its associated service time distribution.
 *
 */
public enum AbandonmentModelingScheme { EXPONENTIAL_SILENT_CONSIDERED_SERVED, SINGLE_KNOWN_AND_CONV_END_FROM_DATA, EXPONENTIAL_SILENT_MARKED, STATE_DEPENDENT_SERVICE_RATE }
