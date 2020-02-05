<?php
class HelloWorldTest extends PHPUnit\Framework\TestCase
{
    public function testExpectHelloWorldActualHelloWorld()
    {
        $this->expectOutputString('Hello World');
		include 'HelloWorld.php';
    }
}