-- How many times starting the job at the provider was tried (transient failures are retried a few times).
ALTER TABLE virtual_try_on_jobs ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0;
