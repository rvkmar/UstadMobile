//
//  UstadBaseViewController.m
//  UstadMobileIOS
//
//  Created by Mike Dawson on 07/08/16.
//  Copyright © 2016 UstadMobile FZ-LLC. All rights reserved.
//

#import "UstadBaseViewController.h"

@interface UstadBaseViewController ()

@end

@implementation UstadBaseViewController

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view.
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)refreshCatalogWithInt:(jint)column {
    //not implemented yet
}

- (void)setClassListVisibleWithBoolean:(jboolean)visible {
    //class list is not present on iOS
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender {
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

@end
