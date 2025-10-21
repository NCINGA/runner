Thread.start {
    def timer = new Timer()

    def task = new TimerTask() {
        @Override
        void run() {
            println "Job executed at: ${new Date()}"
        }
    }

    timer.scheduleAtFixedRate(task, 0, 5000)
    println "Scheduler started."
}

