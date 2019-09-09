package ed_simulation;

/**
 * Schemes are as follows:
 * SINGLE_SERVICE_RATE - there's a single RV for all the service times. It may apply to an exchange or an entire conversation.
 * STATE_DEPENDENT_SERVICE_RATE - basically the state is the time in queue (should be TTFR, though!). Per each state, specify a different service rate (service time is
 * still assumed exponential). We can work with two states - abandoned/not abandoned, or multiple ones, in which each ttfr has its associated service time distribution.
 *
 */
public enum ServiceRateModelingScheme { SINGLE_SERVICE_RATE , STATE_DEPENDENT_SERVICE_RATE }
