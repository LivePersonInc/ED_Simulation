package ed_simulation;

//in TWO_INFTY_QUEUES mode, each server is assumed to work, instead of in Erlang-R mode, as a two M/M/\infty/ network, in which
//A conversation ending either content or needy phase goes immediately into the complementary phase. This enables avoiding
// the calculation of the wait time in the internal queue, and allows using only the average Exchange duration (since in mode
//WITH_WAITING_QUEUE, the exchange duration depends on the internal queue size. Tough more accurate mode, it requires more complex data
//extraction.
public enum ServerWaitingQueueMode { WITH_WAITING_QUEUE, TWO_INFTY_QUEUES }
