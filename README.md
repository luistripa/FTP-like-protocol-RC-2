# RC Assignment 2 - FTP-like protocol

This project was programmed for the second assignment of the 2021 Redes de Computadores subject.

We were assigned a task to transfer a file using several methods of FTP-like packet transfering including GoBackN, Selective Repeat and one of the previous with adaptive timeouts based on the packet RTT and jitter.

The underlying simulator, that handles the packet transport, was created by professor José Legatheaux and can be found [here](https://github.com/jlegatheaux/cnss)

# Config File Structure

The configuration files can be found in the `configs` folder.

## Nodes

To create a node, you must add a line with the following structure:

`node <id> <interfaces> <control-class> <application-class> <arg1> ... <argn>`

where:
- `<id>` is the integer id of the node (starting in 0 and following a strict increasing order);
- `<interfaces>` is the number of network interfaces attached to the node;
- `<control-class>` is the class providing the default control logic for the node;
- `<application-class>` is the class that implements the node application logic;
- `<arg1>` ... is a space separated list of string arguments passed to the previous class.

In our case, the sender uses <b>at most 3 args</b> that represent (in order):

- the name of the file to transfer
- the timeout
- the window size

The receiver always has 1 arg representing the total window size

## Links
To define a link between two nodes, you must do the following:

`link <id1>.<interface1> <id2>.<interface2> <bandwidth> <latency> <error_rate> <jitter>`

where:

- `<id1>` and `<id2>` identify the end nodes of the link;
- `<interface1>` and `<interface2>` identify the two interfaces linked together by the link;
- `<bandwidth>` is the transmission rate of the link in bps;
- `<latency>` is the latency of the link in ms;
- `<error_rate>` is the error rate in percent;
- `<jitter>` is the jitter rate of the link.

