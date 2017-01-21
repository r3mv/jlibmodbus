package com.invertor.examples.modbus;

import com.invertor.modbus.*;
import com.invertor.modbus.data.DataHolder;
import com.invertor.modbus.data.FifoQueue;
import com.invertor.modbus.data.SimpleDataHolderBuilder;
import com.invertor.modbus.data.events.ModbusEventSend;
import com.invertor.modbus.data.mei.ReadDeviceIdentificationInterface;
import com.invertor.modbus.exception.IllegalDataAddressException;
import com.invertor.modbus.exception.IllegalDataValueException;
import com.invertor.modbus.exception.ModbusIOException;
import com.invertor.modbus.master.ModbusMasterTCP;
import com.invertor.modbus.msg.base.mei.MEIReadDeviceIdentification;
import com.invertor.modbus.msg.base.mei.ReadDeviceIdentificationCode;
import com.invertor.modbus.serial.SerialPort;
import jssc.SerialPortList;

import java.net.InetAddress;
import java.nio.charset.Charset;

import static com.invertor.modbus.data.mei.ReadDeviceIdentificationInterface.DataObject;

/*
 * Copyright (C) 2016 "Invertor" Factory", JSC
 * All rights reserved
 *
 * This file is part of JLibModbus.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse
 * or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Authors: Vladislav Y. Kochedykov, software engineer.
 * email: vladislav.kochedykov@gmail.com
 */
public class ModbusTest implements Runnable {

    private static final String DEFAULT_HOST = "localhost";
    private ModbusMaster master;
    private ModbusSlave slave;
    private long timeout = 0;

    static private <T> T initParameter(String title, T parameter, String arg, ParameterInitializer<T> pi) {
        try {
            parameter = pi.init(arg);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.out.format("Invalid %s value:%s%n", title, arg);
        }
        return parameter;
    }

    public static void main(String[] argv) {

        if (argv.length < 1) {
            printUsage();
            return;
        }

        ModbusTest test = new ModbusTest();

        try {
            switch (TransportType.get(argv[0])) {

                case TCP:
                    String host = DEFAULT_HOST;
                    int port = Modbus.TCP_PORT;
                    boolean keepAlive = true;
                    try {
                        host = initParameter("host", host, argv[1], new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return InetAddress.getByName(arg).getHostAddress();
                            }
                        });
                        port = initParameter("port", port, argv[2], new ParameterInitializer<Integer>() {
                            @Override
                            public Integer init(String arg) throws Exception {
                                return Integer.decode(arg);
                            }
                        });
                        keepAlive = initParameter("keepAlive", keepAlive, argv[3], new ParameterInitializer<Boolean>() {
                            @Override
                            public Boolean init(String arg) throws Exception {
                                return Boolean.parseBoolean(arg);
                            }
                        });
                    } catch (IndexOutOfBoundsException ie) {
                        //it's ok
                    }
                    System.out.format("Starting Modbus TCP with settings:%n\t%s, %s, %s%n", host, port, keepAlive);
                    test.master = ModbusMasterFactory.createModbusMasterTCP(host, port, keepAlive);
                    test.slave = ModbusSlaveFactory.createModbusSlaveTCP(host, port);
                    break;
                case RTU:
                    String device_name_slave = SerialPortList.getPortNames()[0];
                    String device_name_master = SerialPortList.getPortNames()[1];
                    SerialPort.BaudRate baud_rate = SerialPort.BaudRate.BAUD_RATE_115200;
                    int data_bits = 8;
                    int stop_bits = 1;
                    SerialPort.Parity parity = SerialPort.Parity.NONE;

                    try {
                        device_name_slave = initParameter("device_name_slave", device_name_slave, argv[1], new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return arg;
                            }
                        });
                        device_name_master = initParameter("device_name_master", device_name_master, argv[2], new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return arg;
                            }
                        });
                        baud_rate = initParameter("baud_rate", baud_rate, argv[3], new ParameterInitializer<SerialPort.BaudRate>() {
                            @Override
                            public SerialPort.BaudRate init(String arg) throws Exception {
                                return SerialPort.BaudRate.getBaudRate(Integer.decode(arg));
                            }
                        });
                        data_bits = initParameter("data_bits", data_bits, argv[4], new ParameterInitializer<Integer>() {
                            @Override
                            public Integer init(String arg) throws Exception {
                                return Integer.decode(arg);
                            }
                        });
                        stop_bits = initParameter("stop_bits", data_bits, argv[5], new ParameterInitializer<Integer>() {
                            @Override
                            public Integer init(String arg) throws Exception {
                                return Integer.decode(arg);
                            }
                        });
                        parity = initParameter("stop_bits", parity, argv[6], new ParameterInitializer<SerialPort.Parity>() {
                            @Override
                            public SerialPort.Parity init(String arg) throws Exception {
                                return SerialPort.Parity.getParity(Integer.decode(arg));
                            }
                        });
                    } catch (IndexOutOfBoundsException ie) {
                        //it's ok
                    }
                    System.out.format("Starting Modbus RTU with settings:%n\t%s, %s, %d, %d, %s%n",
                            device_name_slave, baud_rate.toString(), data_bits, stop_bits, parity.toString());
                    test.master = ModbusMasterFactory.createModbusMasterRTU(device_name_master, baud_rate, data_bits, stop_bits, parity);
                    test.slave = ModbusSlaveFactory.createModbusSlaveRTU(device_name_slave, baud_rate, data_bits, stop_bits, parity);
                    break;
                case ASCII:
                    device_name_slave = SerialPortList.getPortNames()[0];
                    device_name_master = SerialPortList.getPortNames()[1];
                    baud_rate = SerialPort.BaudRate.BAUD_RATE_115200;
                    parity = SerialPort.Parity.ODD;
                    try {
                        device_name_slave = initParameter("device_name_slave", device_name_slave, argv[1], new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return arg;
                            }
                        });
                        device_name_master = initParameter("device_name_master", device_name_master, argv[2], new ParameterInitializer<String>() {
                            @Override
                            public String init(String arg) throws Exception {
                                return arg;
                            }
                        });
                        baud_rate = initParameter("baud_rate", baud_rate, argv[3], new ParameterInitializer<SerialPort.BaudRate>() {
                            @Override
                            public SerialPort.BaudRate init(String arg) throws Exception {
                                return SerialPort.BaudRate.getBaudRate(Integer.decode(arg));
                            }
                        });
                        parity = initParameter("parity", parity, argv[4], new ParameterInitializer<SerialPort.Parity>() {
                            @Override
                            public SerialPort.Parity init(String arg) throws Exception {
                                return SerialPort.Parity.getParity(Integer.decode(arg));
                            }
                        });
                    } catch (IndexOutOfBoundsException ie) {
                        //it's ok
                    }
                    System.out.format("Starting Modbus ASCII with settings:%n\t%s, %s, %s%n",
                            device_name_slave, baud_rate.toString(), parity.toString());
                    test.master = ModbusMasterFactory.createModbusMasterASCII(device_name_master, baud_rate, parity);
                    test.slave = ModbusSlaveFactory.createModbusSlaveASCII(device_name_slave, baud_rate, parity);
                    break;
                default:
                    test.master = ModbusMasterFactory.createModbusMasterTCP(DEFAULT_HOST, false);
                    test.slave = ModbusSlaveFactory.createModbusSlaveTCP(DEFAULT_HOST);

            }
            test.master.setResponseTimeout(1000);

            test.slave.setServerAddress(1);
            test.slave.setDataHolder(new SimpleDataHolderBuilder(1000));
            Modbus.setLogLevel(Modbus.LogLevel.LEVEL_RELEASE);

            try {
                DataHolder dataHolder = test.slave.getDataHolder();
                dataHolder.getCoils().set(1, true);
                dataHolder.getCoils().set(3, true);
                dataHolder.getDiscreteInputs().setRange(0, new boolean[]{false, true, true, false, true});
                dataHolder.getInputRegisters().setRange(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                dataHolder.getInputRegisters().set(11, 69);
                dataHolder.getHoldingRegisters().setRange(0, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
                dataHolder.getSlaveId().set("slave implementation = jlibmodbus".getBytes(Charset.forName("UTF-8")));
                dataHolder.getExceptionStatus().set(123);
                dataHolder.getCommStatus().addEvent(ModbusEventSend.createExceptionSentRead());
                ReadDeviceIdentificationInterface rii = dataHolder.getReadDeviceIdentificationInterface();
                rii.setVendorName("Vendor name=\"JSC Invertor\"");
                rii.setProductCode("Product code=\"3245234658\"");
                rii.setMajorMinorRevision("Revision=\"v1.0\"");
                FifoQueue fifo = dataHolder.getFifoQueue(0);
                for (int i = 0; i < 35; i++) {
                    if (fifo.size() == Modbus.MAX_FIFO_COUNT)
                        fifo.poll();
                    fifo.add(i * 11);
                }
            } catch (IllegalDataAddressException e) {
                e.printStackTrace();
            } catch (IllegalDataValueException e) {
                e.printStackTrace();
            }

            test.start(10000);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.format("Usage: %s [%s, %s, %s]%n", ModbusTest.class.getCanonicalName(), "tcp", "rtu", "ascii");
        System.out.format("\t%s additional parameters:%s %s %s%n\t\t%s%n", "tcp",
                "ip address", "port", "keep_alive(true, false)",
                "Example: 127.0.0.1 502 true");
        System.out.format("\t%s additional parameters:%s %s %s %s %s %s%n\t\t%s%n", "rtu",
                "device_name_slave", "device_name_master", "baud_rate", "data_bits", "stop_bits", "parity(none, odd, even, mark, space)",
                "Example: COM1 115200 8 1 none");
        System.out.format("\t%s additional parameters:%s %s %s %s%n\t\t%s%n", "ascii",
                "device_name_slave", "device_name_master", "baud_rate", "parity(none, odd, even, mark, space)",
                "Example: COM1 115200 odd");
    }

    private static void printRegisters(String title, int[] ir) {
        for (int i : ir)
            System.out.format("%6d", i);
        System.out.format("\t%s%n", title);
    }

    private static void printBits(String title, boolean[] ir) {
        for (boolean i : ir)
            System.out.format("%6s", i);
        System.out.format("\t%s%n", title);
    }

    private void start(long timeout) {
        this.timeout = timeout;
        Thread thread = new Thread(this);
        thread.start();
        try {
            thread.join(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long time = System.currentTimeMillis();
        try {
            slave.listen();
            master.connect();
            master.writeSingleRegister(1, 0, 69);
            Modbus.setAutoIncrementTransactionId(true);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
        master.setResponseTimeout(1000);
        while ((System.currentTimeMillis() - time) < timeout) {
            try {
                Thread.sleep(200);
                System.out.println("Slave output");
                printRegisters("Holding registers", slave.getDataHolder().getHoldingRegisters().getRange(0, 16));
                printRegisters("Input registers", slave.getDataHolder().getInputRegisters().getRange(0, 16));
                printBits("Coils", slave.getDataHolder().getCoils().getRange(0, 16));
                printBits("Discrete inputs", slave.getDataHolder().getDiscreteInputs().getRange(0, 16));
                System.out.println();
                System.out.println("Master output");
                printRegisters("Holding registers", master.readHoldingRegisters(1, 0, 16));
                printRegisters("Input registers", master.readInputRegisters(1, 0, 16));
                printRegisters("Read write registers", master.readWriteMultipleRegisters(1, 0, 16, 3, new int[]{33, 44}));
                printRegisters("Fifo queue registers", master.readFifoQueue(1, 0));
                printBits("Coils", master.readCoils(1, 0, 16));
                printBits("Discrete inputs", master.readDiscreteInputs(1, 0, 16));
                if (!(master instanceof ModbusMasterTCP)) {
                    System.out.format("%s\t\t%s%n", "Slave Id", new String(master.reportSlaveId(1), Charset.defaultCharset()));
                    System.out.format("%s\t\t%d%n", "Exception status", master.readExceptionStatus(1));
                    System.out.format("%s\t\t%d%n", "Comm event counter", master.getCommEventCounter(1).getEventCount());
                    System.out.format("%s\t\t%d%n", "Comm message count", master.getCommEventLog(1).getMessageCount());
                    System.out.format("%s\t\t\t\t%d%n", "Diagnostics", master.diagnosticsReturnBusMessageCount(1));
                }
                master.maskWriteRegister(1, 0, 7, 10);
                master.writeSingleCoil(1, 13, true);
                master.writeMultipleRegisters(1, 5, new int[]{55, 66, 77, 88, 99});
                master.writeMultipleCoils(1, 0, new boolean[]{true, true, true});
                MEIReadDeviceIdentification rdi = master.readDeviceIdentification(1, 0, ReadDeviceIdentificationCode.BASIC_STREAM_ACCESS);
                DataObject[] objects = rdi.getObjects();
                System.out.format("%s\t", "Device identification");
                for (DataObject o : objects) {
                    System.out.format("%s ", new String(o.getValue(), Charset.defaultCharset()));
                }
                System.out.println();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            slave.shutdown();
            master.disconnect();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        }
    }

    private enum TransportType {
        TCP("tcp"),
        RTU("rtu"),
        ASCII("ascii");

        final private String type;

        TransportType(String type) {
            this.type = type;
        }

        static public TransportType get(String s) {
            for (TransportType type : TransportType.values()) {
                if (type.toString().equalsIgnoreCase(s))
                    return type;
            }
            return TCP;
        }

        @Override
        final public String toString() {
            return type;
        }
    }

    interface ParameterInitializer<T> {
        T init(String arg) throws Exception;
    }
}
