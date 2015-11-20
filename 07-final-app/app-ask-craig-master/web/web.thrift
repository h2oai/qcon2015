namespace js App

struct Job {
  1: required string title;
  2: optional string category;
}

service Web {
  string ping();
  string echo(1: string message);
  void createJob(1: Job job);
  list<Job> listJobs(1: i32 skip, 2: i32 limit);
  string predictJobCategory(1: string jobTitle);
}

