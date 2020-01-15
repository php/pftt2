<?php
class HelloWorldTest extends PHPUnit_Framework_TestCase
{
    public function testExpectHelloWorldActualHelloWorld()
    {
        $this->expectOutputString('Hello World');
		include 'HelloWorld.php';
    }
}