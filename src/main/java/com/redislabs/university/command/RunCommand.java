package com.redislabs.university.command;

import com.redislabs.university.examples.Hello;
import io.dropwizard.cli.Command;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

public class RunCommand extends Command {
    public RunCommand() {
        super("run", "Run the specified ");
    }

    @Override
    public void configure(Subparser subparser) {
        subparser.addArgument("--host")
                .dest("host")
                .type(String.class)
                .required(false)
                .setDefault("localhost")
                .help("The host of the Redis server to connect to");

        subparser.addArgument("--port")
                .dest("port")
                .type(Integer.class)
                .required(false)
                .setDefault(6379)
                .help("The port of the Redis server to connect to");

        subparser.addArgument("--example")
                .dest("examples")
                .type(String.class)
                .required(true)
                .help("The name of the examples class to run.");
    }

    @Override
    public void run(Bootstrap<?> bootstrap, Namespace namespace) {
        final String host = namespace.get("host");
        final Integer port = namespace.get("port");
        final String example = ((String) namespace.get("examples")).toLowerCase();
        if ("hello".equals(example)) {
            final Hello hello = new Hello(host, port);
            hello.say();
        } else {
            System.out.println("Unknown example: " + example);
        }
    }
}
